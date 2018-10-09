package net.consensys.pantheon.ethereum.core;

import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.LogManager.getLogger;

import net.consensys.pantheon.ethereum.ProtocolContext;
import net.consensys.pantheon.ethereum.chain.BlockAddedEvent;
import net.consensys.pantheon.ethereum.chain.BlockAddedObserver;
import net.consensys.pantheon.ethereum.chain.Blockchain;
import net.consensys.pantheon.ethereum.chain.MutableBlockchain;
import net.consensys.pantheon.ethereum.mainnet.ProtocolSchedule;
import net.consensys.pantheon.ethereum.mainnet.TransactionValidator;
import net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason;
import net.consensys.pantheon.ethereum.mainnet.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

/**
 * Maintains the set of pending transactions received from JSON-RPC or other nodes. Transactions are
 * removed automatically when they are included in a block on the canonical chain and re-added if a
 * re-org removes them from the canonical chain again.
 *
 * <p>This class is safe for use across multiple threads.
 */
public class TransactionPool implements BlockAddedObserver {
  private static final Logger LOG = getLogger();
  private final PendingTransactions pendingTransactions;
  private final ProtocolSchedule<?> protocolSchedule;
  private final ProtocolContext<?> protocolContext;
  private final TransactionBatchAddedListener transactionBatchAddedListener;

  public TransactionPool(
      final PendingTransactions pendingTransactions,
      final ProtocolSchedule<?> protocolSchedule,
      final ProtocolContext<?> protocolContext,
      final TransactionBatchAddedListener transactionBatchAddedListener) {
    this.pendingTransactions = pendingTransactions;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.transactionBatchAddedListener = transactionBatchAddedListener;
  }

  public ValidationResult<TransactionInvalidReason> addLocalTransaction(
      final Transaction transaction) {
    final ValidationResult<TransactionInvalidReason> validationResult =
        validateTransaction(transaction);

    validationResult.ifValid(
        () -> {
          final boolean added = pendingTransactions.addLocalTransaction(transaction);
          if (added) {
            transactionBatchAddedListener.onTransactionsAdded(singletonList(transaction));
          }
        });
    return validationResult;
  }

  public void addRemoteTransactions(final Collection<Transaction> transactions) {
    final Set<Transaction> addedTransactions = new HashSet<>();
    for (final Transaction transaction : sortByNonce(transactions)) {
      final ValidationResult<TransactionInvalidReason> validationResult =
          validateTransaction(transaction);
      if (validationResult.isValid()) {
        final boolean added = pendingTransactions.addRemoteTransaction(transaction);
        if (added) {
          addedTransactions.add(transaction);
        }
      } else {
        LOG.debug(
            "Validation failed ({}) for transaction {}. Discarding.",
            validationResult.getInvalidReason(),
            transaction);
      }
    }
    if (!addedTransactions.isEmpty()) {
      transactionBatchAddedListener.onTransactionsAdded(addedTransactions);
    }
  }

  // Sort transactions by nonce to ensure we import sequences of transactions correctly
  private List<Transaction> sortByNonce(final Collection<Transaction> transactions) {
    final List<Transaction> sortedTransactions = new ArrayList<>(transactions);
    sortedTransactions.sort(Comparator.comparing(Transaction::getNonce));
    return sortedTransactions;
  }

  public void addTransactionListener(final PendingTransactionListener listener) {
    pendingTransactions.addTransactionListener(listener);
  }

  @Override
  public void onBlockAdded(final BlockAddedEvent event, final Blockchain blockchain) {
    event.getAddedTransactions().forEach(pendingTransactions::removeTransaction);
    addRemoteTransactions(event.getRemovedTransactions());
  }

  private TransactionValidator getTransactionValidator() {
    return protocolSchedule
        .getByBlockNumber(protocolContext.getBlockchain().getChainHeadBlockNumber())
        .getTransactionValidator();
  }

  public PendingTransactions getPendingTransactions() {
    return pendingTransactions;
  }

  private ValidationResult<TransactionInvalidReason> validateTransaction(
      final Transaction transaction) {
    final ValidationResult<TransactionInvalidReason> basicValidationResult =
        getTransactionValidator().validate(transaction);
    if (!basicValidationResult.isValid()) {
      return basicValidationResult;
    }

    final BlockHeader chainHeadBlockHeader = getChainHeadBlockHeader();
    if (transaction.getGasLimit() > chainHeadBlockHeader.getGasLimit()) {
      return ValidationResult.invalid(
          TransactionInvalidReason.EXCEEDS_BLOCK_GAS_LIMIT,
          String.format(
              "Transaction gas limit of %s exceeds block gas limit of %s",
              transaction.getGasLimit(), chainHeadBlockHeader.getGasLimit()));
    }

    return getTransactionValidator()
        .validateForSender(
            transaction,
            getSenderAccount(transaction, chainHeadBlockHeader),
            pendingTransactions.getNextNonceForSender(transaction.getSender()));
  }

  private Account getSenderAccount(
      final Transaction transaction, final BlockHeader chainHeadHeader) {
    final WorldState worldState =
        protocolContext.getWorldStateArchive().get(chainHeadHeader.getStateRoot());
    return worldState.get(transaction.getSender());
  }

  private BlockHeader getChainHeadBlockHeader() {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    return blockchain.getBlockHeader(blockchain.getChainHeadHash()).get();
  }

  public interface TransactionBatchAddedListener {

    void onTransactionsAdded(Iterable<Transaction> transactions);
  }
}
