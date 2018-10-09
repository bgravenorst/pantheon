package net.consensys.pantheon.consensus.clique;

import net.consensys.pantheon.crypto.SECP256K1.Signature;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.core.Util;
import net.consensys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import net.consensys.pantheon.util.bytes.BytesValue;

import java.util.function.Supplier;

public class CliqueBlockHashing {

  /**
   * Constructs a hash of the block header, suitable for use when creating the proposer seal. The
   * extra data is modified to have a null proposer seal and empty list of committed seals.
   *
   * @param header The header for which a proposer seal is to be calculated
   * @param cliqueExtraData The extra data block which is to be inserted to the header once seal is
   *     calculated
   * @return the hash of the header suitable for signing as the proposer seal
   */
  public static Hash calculateDataHashForProposerSeal(
      final BlockHeader header, final CliqueExtraData cliqueExtraData) {
    final BytesValue headerRlp = serializeHeaderWithoutProposerSeal(header, cliqueExtraData);
    return Hash.hash(headerRlp); // Proposer hash is the hash of the RLP
  }

  /**
   * Recovers the proposer's {@link Address} from the proposer seal.
   *
   * @param header the block header that was signed by the proposer seal
   * @param cliqueExtraData the parsed CliqueExtraData from the header
   * @return the proposer address
   */
  public static Address recoverProposerAddress(
      final BlockHeader header, final CliqueExtraData cliqueExtraData) {
    if (!cliqueExtraData.getProposerSeal().isPresent()) {
      throw new IllegalArgumentException(
          "Supplied cliqueExtraData does not include a proposer " + "seal");
    }
    final Hash proposerHash = calculateDataHashForProposerSeal(header, cliqueExtraData);
    return Util.signatureToAddress(cliqueExtraData.getProposerSeal().get(), proposerHash);
  }

  private static BytesValue serializeHeaderWithoutProposerSeal(
      final BlockHeader header, final CliqueExtraData cliqueExtraData) {
    return serializeHeader(header, () -> encodeExtraDataWithoutProposerSeal(cliqueExtraData));
  }

  private static BytesValue encodeExtraDataWithoutProposerSeal(
      final CliqueExtraData cliqueExtraData) {
    final BytesValue extraDataBytes = cliqueExtraData.encode();
    // Always trim off final 65 bytes (which maybe zeros)
    return extraDataBytes.slice(0, extraDataBytes.size() - Signature.BYTES_REQUIRED);
  }

  private static BytesValue serializeHeader(
      final BlockHeader header, final Supplier<BytesValue> extraDataSerializer) {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();

    out.writeBytesValue(header.getParentHash());
    out.writeBytesValue(header.getOmmersHash());
    out.writeBytesValue(header.getCoinbase());
    out.writeBytesValue(header.getStateRoot());
    out.writeBytesValue(header.getTransactionsRoot());
    out.writeBytesValue(header.getReceiptsRoot());
    out.writeBytesValue(header.getLogsBloom().getBytes());
    out.writeUInt256Scalar(header.getDifficulty());
    out.writeLongScalar(header.getNumber());
    out.writeLongScalar(header.getGasLimit());
    out.writeLongScalar(header.getGasUsed());
    out.writeLongScalar(header.getTimestamp());
    out.writeBytesValue(extraDataSerializer.get());
    out.writeBytesValue(header.getMixHash());
    out.writeLong(header.getNonce());
    out.endList();
    return out.encoded();
  }
}
