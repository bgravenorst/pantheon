package net.consensys.pantheon.consensus.ibft.blockcreation;

import net.consensys.pantheon.consensus.ibft.IbftBlockHashing;
import net.consensys.pantheon.consensus.ibft.IbftContext;
import net.consensys.pantheon.consensus.ibft.IbftExtraData;
import net.consensys.pantheon.consensus.ibft.IbftHelpers;
import net.consensys.pantheon.crypto.SECP256K1;
import net.consensys.pantheon.crypto.SECP256K1.KeyPair;
import net.consensys.pantheon.ethereum.ProtocolContext;
import net.consensys.pantheon.ethereum.blockcreation.AbstractBlockCreator;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.core.BlockHashFunction;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.BlockHeaderBuilder;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.core.PendingTransactions;
import net.consensys.pantheon.ethereum.core.SealableBlockHeader;
import net.consensys.pantheon.ethereum.core.Util;
import net.consensys.pantheon.ethereum.core.Wei;
import net.consensys.pantheon.ethereum.mainnet.ProtocolSchedule;
import net.consensys.pantheon.ethereum.mainnet.ScheduleBasedBlockHashFunction;

import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for producing a Block which conforms to IBFT validation rules (other than missing
 * commit seals). Transactions and associated Hashes (stateroot, receipts etc.) are loaded into the
 * Block in the base class as part of the transaction selection process.
 */
public class IbftBlockCreator extends AbstractBlockCreator<IbftContext> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final KeyPair nodeKeys;
  private final ProtocolSchedule<IbftContext> protocolSchedule;

  public IbftBlockCreator(
      final Address coinbase,
      final ExtraDataCalculator extraDataCalculator,
      final PendingTransactions pendingTransactions,
      final ProtocolContext<IbftContext> protocolContext,
      final ProtocolSchedule<IbftContext> protocolSchedule,
      final Function<Long, Long> gasLimitCalculator,
      final KeyPair nodeKeys,
      final Wei minTransactionGasPrice,
      final BlockHeader parentHeader) {
    super(
        coinbase,
        extraDataCalculator,
        pendingTransactions,
        protocolContext,
        protocolSchedule,
        gasLimitCalculator,
        minTransactionGasPrice,
        Util.publicKeyToAddress(nodeKeys.getPublicKey()),
        parentHeader);
    this.nodeKeys = nodeKeys;
    this.protocolSchedule = protocolSchedule;
  }

  /**
   * Responsible for signing (hash of) the block (including MixHash and Nonce), and then injecting
   * the seal into the extraData. This is called after a suitable set of transactions have been
   * identified, and all resulting hashes have been inserted into the passed-in SealableBlockHeader.
   *
   * @param sealableBlockHeader A block header containing StateRoots, TransactionHashes etc.
   * @return The blockhead which is to be added to the block being proposed.
   */
  @Override
  protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {

    final BlockHashFunction blockHashFunction =
        ScheduleBasedBlockHashFunction.create(protocolSchedule);

    final BlockHeaderBuilder builder =
        BlockHeaderBuilder.create()
            .populateFrom(sealableBlockHeader)
            .mixHash(IbftHelpers.EXPECTED_MIX_HASH)
            .nonce(0)
            .blockHashFunction(blockHashFunction);

    final IbftExtraData sealedExtraData = constructSignedExtraData(builder.buildBlockHeader());

    // Replace the extraData in the BlockHeaderBuilder, and return header.
    return builder.extraData(sealedExtraData.encode()).buildBlockHeader();
  }

  /**
   * Produces an IbftExtraData object with a populated proposerSeal. The signature in the block is
   * generated from the Hash of the header (minus proposer and committer seals) and the nodeKeys.
   *
   * @param headerToSign An almost fully populated header (proposer and committer seals are empty)
   * @return Extra data containing the same vanity data and validators as extraData, however
   *     proposerSeal will also be populated.
   */
  private IbftExtraData constructSignedExtraData(final BlockHeader headerToSign) {
    final IbftExtraData extraData = IbftExtraData.decode(headerToSign.getExtraData());
    final Hash hashToSign =
        IbftBlockHashing.calculateDataHashForProposerSeal(headerToSign, extraData);
    return new IbftExtraData(
        extraData.getVanityData(),
        extraData.getSeals(),
        SECP256K1.sign(hashToSign, nodeKeys),
        extraData.getValidators());
  }
}
