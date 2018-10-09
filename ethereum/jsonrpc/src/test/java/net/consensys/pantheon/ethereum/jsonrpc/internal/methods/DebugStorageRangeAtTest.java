package net.consensys.pantheon.ethereum.jsonrpc.internal.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.consensys.pantheon.ethereum.chain.Blockchain;
import net.consensys.pantheon.ethereum.core.Account;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.core.MutableWorldState;
import net.consensys.pantheon.ethereum.core.Transaction;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import net.consensys.pantheon.ethereum.jsonrpc.internal.processor.BlockReplay;
import net.consensys.pantheon.ethereum.jsonrpc.internal.processor.BlockReplay.Action;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.TransactionWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.DebugStorageRangeAtResult;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.DebugStorageRangeAtResult.StorageEntry;
import net.consensys.pantheon.ethereum.mainnet.TransactionProcessor;
import net.consensys.pantheon.util.bytes.Bytes32;
import net.consensys.pantheon.util.uint.UInt256;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

public class DebugStorageRangeAtTest {

  private static final int TRANSACTION_INDEX = 2;
  private static final Bytes32 START_KEY_HASH = Bytes32.fromHexString("0x22");
  private final JsonRpcParameter parameters = new JsonRpcParameter();
  private final Blockchain blockchain = mock(Blockchain.class);
  private final BlockchainQueries blockchainQueries = mock(BlockchainQueries.class);
  private final BlockReplay blockReplay = mock(BlockReplay.class);
  private final DebugStorageRangeAt debugStorageRangeAt =
      new DebugStorageRangeAt(parameters, blockchainQueries, blockReplay);
  private final MutableWorldState worldState = mock(MutableWorldState.class);
  private final Account account = mock(Account.class);
  private final TransactionProcessor transactionProcessor = mock(TransactionProcessor.class);
  private final Transaction transaction = mock(Transaction.class);

  private final BlockHeader blockHeader = mock(BlockHeader.class);
  private final Hash blockHash =
      Hash.fromHexString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
  private final Hash transactionHash =
      Hash.fromHexString("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
  private final Address accountAddress = Address.MODEXP;

  @Before
  public void setUp() {
    when(transaction.hash()).thenReturn(transactionHash);
  }

  @Test
  public void nameShouldBeDebugStorageRangeAt() {
    assertEquals("debug_storageRangeAt", debugStorageRangeAt.getName());
  }

  @Test
  public void shouldRetrieveStorageRange() {
    final TransactionWithMetadata transactionWithMetadata =
        new TransactionWithMetadata(transaction, 12L, blockHash, TRANSACTION_INDEX);
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_storageRangeAt",
            new Object[] {
              blockHash.toString(), TRANSACTION_INDEX, accountAddress, START_KEY_HASH.toString(), 10
            });

    when(blockchainQueries.transactionByBlockHashAndIndex(blockHash, TRANSACTION_INDEX))
        .thenReturn(transactionWithMetadata);
    when(worldState.get(accountAddress)).thenReturn(account);
    when(blockReplay.afterTransactionInBlock(eq(blockHash), eq(transactionHash), any()))
        .thenAnswer(this::callAction);
    final NavigableMap<Bytes32, UInt256> rawEntries = new TreeMap<>();
    rawEntries.put(Bytes32.fromHexString("0x33"), UInt256.of(6));
    rawEntries.put(Bytes32.fromHexString("0x44"), UInt256.of(7));
    when(account.storageEntriesFrom(START_KEY_HASH, 11)).thenReturn(rawEntries);
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugStorageRangeAt.response(request);
    final DebugStorageRangeAtResult result = (DebugStorageRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getNextKey()).isNull();
    assertThat(result.getStorage())
        .containsExactly(
            entry(Bytes32.fromHexString("0x33").toString(), new StorageEntry(UInt256.of(6))),
            entry(Bytes32.fromHexString("0x44").toString(), new StorageEntry(UInt256.of(7))));
  }

  private Object callAction(final InvocationOnMock invocation) {
    return Optional.of(
        ((Action) invocation.getArgument(2))
            .performAction(transaction, blockHeader, blockchain, worldState, transactionProcessor));
  }
}
