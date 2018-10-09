package net.consensys.pantheon.ethereum.mainnet;

import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.INCORRECT_NONCE;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.INTRINSIC_GAS_EXCEEDS_GAS_LIMIT;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.INVALID_SIGNATURE;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.NONCE_TOO_LOW;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.REPLAY_PROTECTED_SIGNATURES_NOT_SUPPORTED;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.UPFRONT_COST_EXCEEDS_BALANCE;
import static net.consensys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.WRONG_CHAIN_ID;

import net.consensys.pantheon.crypto.SECP256K1;
import net.consensys.pantheon.ethereum.core.Account;
import net.consensys.pantheon.ethereum.core.Gas;
import net.consensys.pantheon.ethereum.core.Transaction;
import net.consensys.pantheon.ethereum.vm.GasCalculator;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Validates a transaction based on Frontier protocol runtime requirements.
 *
 * <p>The {@link MainnetTransactionValidator} performs the intrinsic gas cost check on the given
 * {@link Transaction}.
 */
public class MainnetTransactionValidator implements TransactionValidator {

  public static final int NO_CHAIN_ID = -1;

  public static MainnetTransactionValidator create() {
    return new MainnetTransactionValidator(new FrontierGasCalculator(), false);
  }

  private final GasCalculator gasCalculator;

  private final boolean disallowSignatureMalleability;

  private final OptionalInt chainId;

  public MainnetTransactionValidator(
      final GasCalculator gasCalculator, final boolean checkSignatureMalleability) {
    this(gasCalculator, checkSignatureMalleability, NO_CHAIN_ID);
  }

  public MainnetTransactionValidator(
      final GasCalculator gasCalculator,
      final boolean checkSignatureMalleability,
      final int chainId) {
    this.gasCalculator = gasCalculator;
    this.disallowSignatureMalleability = checkSignatureMalleability;
    this.chainId = chainId > 0 ? OptionalInt.of(chainId) : OptionalInt.empty();
  }

  @Override
  public ValidationResult<TransactionInvalidReason> validate(final Transaction transaction) {
    final ValidationResult<TransactionInvalidReason> signatureResult =
        validateTransactionSignature(transaction);
    if (!signatureResult.isValid()) {
      return signatureResult;
    }

    final Gas intrinsicGasCost = gasCalculator.transactionIntrinsicGasCost(transaction);
    if (intrinsicGasCost.compareTo(Gas.of(transaction.getGasLimit())) > 0) {
      return ValidationResult.invalid(
          INTRINSIC_GAS_EXCEEDS_GAS_LIMIT,
          String.format(
              "intrinsic gas cost %s exceeds gas limit %s",
              intrinsicGasCost, transaction.getGasLimit()));
    }

    return ValidationResult.valid();
  }

  @Override
  public ValidationResult<TransactionInvalidReason> validateForSender(
      final Transaction transaction, final Account sender, final OptionalLong maximumNonce) {
    if (sender == null) {
      return ValidationResult.invalid(UPFRONT_COST_EXCEEDS_BALANCE, "Unknown sender account");
    }
    if (transaction.getUpfrontCost().compareTo(sender.getBalance()) > 0) {
      return ValidationResult.invalid(
          UPFRONT_COST_EXCEEDS_BALANCE,
          String.format(
              "transaction up-front cost %s exceeds transaction sender account balance %s",
              transaction.getUpfrontCost(), sender.getBalance()));
    }

    if (transaction.getNonce() < sender.getNonce()) {
      return ValidationResult.invalid(
          NONCE_TOO_LOW,
          String.format(
              "transaction nonce %s below sender account nonce %s",
              transaction.getNonce(), sender.getNonce()));
    }

    if (violatesMaximumNonce(transaction, maximumNonce)
        && sender.getNonce() != transaction.getNonce()) {
      return ValidationResult.invalid(
          INCORRECT_NONCE,
          String.format(
              "transaction nonce %s does not match sender account nonce %s.",
              transaction.getNonce(), sender.getNonce()));
    }

    return ValidationResult.valid();
  }

  private boolean violatesMaximumNonce(
      final Transaction transaction, final OptionalLong maximumNonce) {
    return !maximumNonce.isPresent() || transaction.getNonce() > maximumNonce.getAsLong();
  }

  public ValidationResult<TransactionInvalidReason> validateTransactionSignature(
      final Transaction transaction) {
    if (chainId.isPresent()
        && (transaction.getChainId().isPresent() && !transaction.getChainId().equals(chainId))) {
      return ValidationResult.invalid(
          WRONG_CHAIN_ID,
          String.format(
              "transaction was meant for chain id %s and not this chain id %s",
              transaction.getChainId().getAsInt(), chainId.getAsInt()));
    }

    if (!chainId.isPresent() && transaction.getChainId().isPresent()) {
      return ValidationResult.invalid(
          REPLAY_PROTECTED_SIGNATURES_NOT_SUPPORTED,
          "replay protected signatures is not supported");
    }

    final SECP256K1.Signature signature = transaction.getSignature();
    if (disallowSignatureMalleability
        && signature.getS().compareTo(SECP256K1.HALF_CURVE_ORDER) > 0) {
      return ValidationResult.invalid(
          INVALID_SIGNATURE,
          String.format(
              "Signature s value should be less than %s, but got %s",
              SECP256K1.HALF_CURVE_ORDER, signature.getS()));
    }

    // org.bouncycastle.math.ec.ECCurve.AbstractFp.decompressPoint throws an
    // IllegalArgumentException for "Invalid point compression" for bad signatures.
    try {
      transaction.getSender();
    } catch (final IllegalArgumentException e) {
      return ValidationResult.invalid(
          INVALID_SIGNATURE, "sender could not be extracted from transaction signature");
    }

    return ValidationResult.valid();
  }
}
