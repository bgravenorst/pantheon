package net.consensys.pantheon.ethereum.eth.sync.tasks;

import net.consensys.pantheon.ethereum.ProtocolContext;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.eth.manager.AbstractEthTask;
import net.consensys.pantheon.ethereum.eth.manager.AbstractPeerTask;
import net.consensys.pantheon.ethereum.eth.manager.EthContext;
import net.consensys.pantheon.ethereum.eth.manager.EthPeer;
import net.consensys.pantheon.ethereum.mainnet.ProtocolSchedule;
import net.consensys.pantheon.ethereum.util.BlockchainUtil;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;

public class DetermineCommonAncestorTask<C> extends AbstractEthTask<BlockHeader> {
  private final EthContext ethContext;
  private final ProtocolSchedule<C> protocolSchedule;
  private final ProtocolContext<C> protocolContext;
  private final EthPeer peer;
  private final int headerRequestSize;

  private long maximumPossibleCommonAncestorNumber;
  private long minimumPossibleCommonAncestorNumber;
  private BlockHeader commonAncestorCandidate;
  private boolean initialQuery = true;

  private DetermineCommonAncestorTask(
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final EthPeer peer,
      final int headerRequestSize) {
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.protocolContext = protocolContext;
    this.peer = peer;
    this.headerRequestSize = headerRequestSize;

    maximumPossibleCommonAncestorNumber = protocolContext.getBlockchain().getChainHeadBlockNumber();
    minimumPossibleCommonAncestorNumber = BlockHeader.GENESIS_BLOCK_NUMBER;
    commonAncestorCandidate =
        protocolContext.getBlockchain().getBlockHeader(BlockHeader.GENESIS_BLOCK_NUMBER).get();
  }

  public static <C> DetermineCommonAncestorTask<C> create(
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final EthPeer peer,
      final int headerRequestSize) {
    return new DetermineCommonAncestorTask<>(
        protocolSchedule, protocolContext, ethContext, peer, headerRequestSize);
  }

  @Override
  protected void executeTask() {
    if (maximumPossibleCommonAncestorNumber == minimumPossibleCommonAncestorNumber) {
      // Bingo, we found our common ancestor.
      result.get().complete(commonAncestorCandidate);
      return;
    }
    if (maximumPossibleCommonAncestorNumber < BlockHeader.GENESIS_BLOCK_NUMBER
        && !result.get().isDone()) {
      result.get().completeExceptionally(new IllegalStateException("No common ancestor."));
      return;
    }
    requestHeaders()
        .thenCompose(this::processHeaders)
        .whenComplete(
            (peerResult, error) -> {
              if (error != null) {
                result.get().completeExceptionally(error);
              } else if (!result.get().isDone()) {
                executeTask();
              }
            });
  }

  @VisibleForTesting
  CompletableFuture<AbstractPeerTask.PeerTaskResult<List<BlockHeader>>> requestHeaders() {
    final long range = maximumPossibleCommonAncestorNumber - minimumPossibleCommonAncestorNumber;
    final int skipInterval = initialQuery ? 0 : calculateSkipInterval(range, headerRequestSize);
    final int count =
        initialQuery ? headerRequestSize : calculateCount((double) range, skipInterval);

    return executeSubTask(
        () ->
            GetHeadersFromPeerByNumberTask.endingAtNumber(
                    protocolSchedule,
                    ethContext,
                    maximumPossibleCommonAncestorNumber,
                    count,
                    skipInterval)
                .assignPeer(peer)
                .run());
  }

  /**
   * In the case where the remote chain contains 100 blocks, the initial count work out to 11, and
   * the skip interval would be 9. This would yield the headers (0, 10, 20, 30, 40, 50, 60, 70, 80,
   * 90, 100).
   */
  @VisibleForTesting
  static int calculateSkipInterval(final long range, final int headerRequestSize) {
    return Math.max(0, Math.toIntExact(range / (headerRequestSize - 1) - 1) - 1);
  }

  @VisibleForTesting
  static int calculateCount(final double range, final int skipInterval) {
    return Math.toIntExact((long) Math.ceil(range / (skipInterval + 1)) + 1);
  }

  private CompletableFuture<Void> processHeaders(
      final AbstractPeerTask.PeerTaskResult<List<BlockHeader>> headersResult) {
    initialQuery = false;
    List<BlockHeader> headers = headersResult.getResult();

    OptionalInt maybeAncestorNumber =
        BlockchainUtil.findHighestKnownBlockIndex(protocolContext.getBlockchain(), headers, false);

    // Means the insertion point is in the next header request.
    if (!maybeAncestorNumber.isPresent()) {
      maximumPossibleCommonAncestorNumber = headers.get(headers.size() - 1).getNumber() - 1L;
      return CompletableFuture.completedFuture(null);
    }
    int ancestorNumber = maybeAncestorNumber.getAsInt();
    commonAncestorCandidate = headers.get(ancestorNumber);

    if (ancestorNumber - 1 >= 0) {
      maximumPossibleCommonAncestorNumber = headers.get(ancestorNumber - 1).getNumber() - 1L;
    }
    minimumPossibleCommonAncestorNumber = headers.get(ancestorNumber).getNumber();

    return CompletableFuture.completedFuture(null);
  }
}
