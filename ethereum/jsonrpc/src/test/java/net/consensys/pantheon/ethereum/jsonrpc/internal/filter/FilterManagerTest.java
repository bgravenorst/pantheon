package net.consensys.pantheon.ethereum.jsonrpc.internal.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import net.consensys.pantheon.ethereum.chain.BlockAddedEvent;
import net.consensys.pantheon.ethereum.chain.Blockchain;
import net.consensys.pantheon.ethereum.core.Block;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.core.Transaction;
import net.consensys.pantheon.ethereum.core.TransactionPool;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import net.consensys.pantheon.ethereum.testutil.BlockDataGenerator;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilterManagerTest {

  private BlockDataGenerator blockGenerator;
  private Block currentBlock;
  private FilterManager filterManager;

  @Mock private Blockchain blockchain;
  @Mock private BlockchainQueries blockchainQueries;
  @Mock private TransactionPool transactionPool;

  @Before
  public void setupTest() {
    when(blockchainQueries.getBlockchain()).thenReturn(blockchain);
    this.blockGenerator = new BlockDataGenerator();
    this.currentBlock = blockGenerator.genesisBlock();
    this.filterManager =
        new FilterManager(blockchainQueries, transactionPool, new FilterIdGenerator());
  }

  @Test
  public void uninstallNonexistentFilter() {
    assertThat(filterManager.uninstallFilter("1")).isFalse();
  }

  @Test
  public void installUninstallNewBlockFilter() {
    assertThat(filterManager.blockFilterCount()).isEqualTo(0);

    final String filterId = filterManager.installBlockFilter();
    assertThat(filterManager.blockFilterCount()).isEqualTo(1);

    assertThat(filterManager.uninstallFilter(filterId)).isTrue();
    assertThat(filterManager.blockFilterCount()).isEqualTo(0);

    assertThat(filterManager.blockChanges(filterId)).isNull();
  }

  @Test
  public void newBlockChanges_SingleFilter() {
    final String filterId = filterManager.installBlockFilter();
    assertThat(filterManager.blockChanges(filterId).size()).isEqualTo(0);

    final Hash blockHash1 = appendBlockToBlockchain();
    final List<Hash> expectedHashes = Lists.newArrayList(blockHash1);
    assertThat(filterManager.blockChanges(filterId)).isEqualTo(expectedHashes);

    // Check that changes have been flushed.
    expectedHashes.clear();
    assertThat(filterManager.blockChanges(filterId)).isEqualTo(expectedHashes);

    final Hash blockHash2 = appendBlockToBlockchain();
    expectedHashes.add(blockHash2);
    final Hash blockHash3 = appendBlockToBlockchain();
    expectedHashes.add(blockHash3);
    assertThat(filterManager.blockChanges(filterId)).isEqualTo(expectedHashes);
  }

  @Test
  public void newBlockChanges_MultipleFilters() {
    final String filterId1 = filterManager.installBlockFilter();
    assertThat(filterManager.blockChanges(filterId1).size()).isEqualTo(0);

    final Hash blockHash1 = appendBlockToBlockchain();
    final List<Hash> expectedHashes1 = Lists.newArrayList(blockHash1);

    final String filterId2 = filterManager.installBlockFilter();
    final Hash blockHash2 = appendBlockToBlockchain();
    expectedHashes1.add(blockHash2);
    final List<Hash> expectedHashes2 = Lists.newArrayList(blockHash2);
    assertThat(filterManager.blockChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.blockChanges(filterId2)).isEqualTo(expectedHashes2);
    expectedHashes1.clear();
    expectedHashes2.clear();

    // Both filters have been flushed.
    assertThat(filterManager.blockChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.blockChanges(filterId2)).isEqualTo(expectedHashes2);

    final Hash blockHash3 = appendBlockToBlockchain();
    expectedHashes1.add(blockHash3);
    expectedHashes2.add(blockHash3);

    // Flush the first filter.
    assertThat(filterManager.blockChanges(filterId1)).isEqualTo(expectedHashes1);
    expectedHashes1.clear();

    final Hash blockHash4 = appendBlockToBlockchain();
    expectedHashes1.add(blockHash4);
    expectedHashes2.add(blockHash4);
    assertThat(filterManager.blockChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.blockChanges(filterId2)).isEqualTo(expectedHashes2);
  }

  @Test
  public void installUninstallPendingTransactionFilter() {
    assertThat(filterManager.pendingTransactionFilterCount()).isEqualTo(0);

    final String filterId = filterManager.installPendingTransactionFilter();
    assertThat(filterManager.pendingTransactionFilterCount()).isEqualTo(1);

    assertThat(filterManager.uninstallFilter(filterId)).isTrue();
    assertThat(filterManager.pendingTransactionFilterCount()).isEqualTo(0);

    assertThat(filterManager.pendingTransactionChanges(filterId)).isNull();
  }

  @Test
  public void getTransactionChanges_SingleFilter() {
    final String filterId = filterManager.installPendingTransactionFilter();
    assertThat(filterManager.pendingTransactionChanges(filterId).size()).isEqualTo(0);

    final Hash transactionHash1 = receivePendingTransaction();
    final List<Hash> expectedHashes = Lists.newArrayList(transactionHash1);
    assertThat(filterManager.pendingTransactionChanges(filterId)).isEqualTo(expectedHashes);

    // Check that changes have been flushed.
    expectedHashes.clear();
    assertThat(filterManager.pendingTransactionChanges(filterId)).isEqualTo(expectedHashes);

    final Hash transactionHash2 = receivePendingTransaction();
    expectedHashes.add(transactionHash2);
    final Hash transactionHash3 = receivePendingTransaction();
    expectedHashes.add(transactionHash3);
    assertThat(filterManager.pendingTransactionChanges(filterId)).isEqualTo(expectedHashes);
  }

  @Test
  public void getPendingTransactionChanges_MultipleFilters() {
    final String filterId1 = filterManager.installPendingTransactionFilter();
    assertThat(filterManager.pendingTransactionChanges(filterId1).size()).isEqualTo(0);

    final Hash transactionHash1 = receivePendingTransaction();
    final List<Hash> expectedHashes1 = Lists.newArrayList(transactionHash1);

    final String filterId2 = filterManager.installPendingTransactionFilter();
    final Hash transactionHash2 = receivePendingTransaction();
    expectedHashes1.add(transactionHash2);
    final List<Hash> expectedHashes2 = Lists.newArrayList(transactionHash2);
    assertThat(filterManager.pendingTransactionChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.pendingTransactionChanges(filterId2)).isEqualTo(expectedHashes2);
    expectedHashes1.clear();
    expectedHashes2.clear();

    // Both filters have been flushed.
    assertThat(filterManager.pendingTransactionChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.pendingTransactionChanges(filterId2)).isEqualTo(expectedHashes2);

    final Hash transactionHash3 = receivePendingTransaction();
    expectedHashes1.add(transactionHash3);
    expectedHashes2.add(transactionHash3);

    // Flush the first filter.
    assertThat(filterManager.pendingTransactionChanges(filterId1)).isEqualTo(expectedHashes1);
    expectedHashes1.clear();

    final Hash transactionHash4 = receivePendingTransaction();
    expectedHashes1.add(transactionHash4);
    expectedHashes2.add(transactionHash4);
    assertThat(filterManager.pendingTransactionChanges(filterId1)).isEqualTo(expectedHashes1);
    assertThat(filterManager.pendingTransactionChanges(filterId2)).isEqualTo(expectedHashes2);
  }

  private Hash appendBlockToBlockchain() {
    final long blockNumber = currentBlock.getHeader().getNumber() + 1;
    final Hash parentHash = currentBlock.getHash();
    final BlockDataGenerator.BlockOptions options =
        new BlockDataGenerator.BlockOptions().setBlockNumber(blockNumber).setParentHash(parentHash);
    currentBlock = blockGenerator.block(options);
    filterManager.recordBlockEvent(
        BlockAddedEvent.createForHeadAdvancement(currentBlock), blockchainQueries.getBlockchain());
    return currentBlock.getHash();
  }

  private Hash receivePendingTransaction() {
    final Transaction transaction = blockGenerator.transaction();
    filterManager.recordPendingTransactionEvent(transaction);
    return transaction.hash();
  }
}
