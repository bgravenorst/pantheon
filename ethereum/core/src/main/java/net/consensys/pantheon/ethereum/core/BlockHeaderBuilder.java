package net.consensys.pantheon.ethereum.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.consensys.pantheon.util.bytes.BytesValue;
import net.consensys.pantheon.util.uint.UInt256;

import java.time.Instant;
import java.util.OptionalLong;

/** A utility class for building block headers. */
public class BlockHeaderBuilder {

  private Hash parentHash;

  private Hash ommersHash;

  private Address coinbase;

  private Hash stateRoot;

  private Hash transactionsRoot;

  private Hash receiptsRoot;

  private LogsBloomFilter logsBloom;

  private UInt256 difficulty;

  private long number = -1L;

  private long gasLimit = -1L;

  private long gasUsed = -1L;

  private long timestamp = -1L;

  private BytesValue extraData;

  private Hash mixHash;

  private BlockHashFunction blockHashFunction;

  // A nonce can be any value so we use the OptionalLong
  // instead of an invalid identifier such as -1.
  private OptionalLong nonce = OptionalLong.empty();

  public static BlockHeaderBuilder create() {
    return new BlockHeaderBuilder();
  }

  public BlockHeader buildBlockHeader() {
    validateBlockHeader();

    return new BlockHeader(
        parentHash,
        ommersHash,
        coinbase,
        stateRoot,
        transactionsRoot,
        receiptsRoot,
        logsBloom,
        difficulty,
        number,
        gasLimit,
        gasUsed,
        timestamp < 0 ? Instant.now().getEpochSecond() : timestamp,
        extraData,
        mixHash,
        nonce.getAsLong(),
        blockHashFunction);
  }

  public ProcessableBlockHeader buildProcessableBlockHeader() {
    validateProcessableBlockHeader();

    return new ProcessableBlockHeader(
        parentHash, coinbase, difficulty, number, gasLimit, timestamp);
  }

  public SealableBlockHeader buildSealableBlockHeader() {
    validateSealableBlockHeader();

    return new SealableBlockHeader(
        parentHash,
        ommersHash,
        coinbase,
        stateRoot,
        transactionsRoot,
        receiptsRoot,
        logsBloom,
        difficulty,
        number,
        gasLimit,
        gasUsed,
        timestamp,
        extraData);
  }

  private void validateBlockHeader() {
    validateSealableBlockHeader();
    checkState(this.mixHash != null, "Missing mixHash");
    checkState(this.nonce.isPresent(), "Missing nonce");
    checkState(this.blockHashFunction != null, "Missing blockHashFunction");
  }

  private void validateProcessableBlockHeader() {
    checkState(this.parentHash != null, "Missing parent hash");
    checkState(this.coinbase != null, "Missing coinbase");
    checkState(this.difficulty != null, "Missing block difficulty");
    checkState(this.number > -1L, "Missing block number");
    checkState(this.gasLimit > -1L, "Missing gas limit");
    checkState(this.timestamp > -1L, "Missing timestamp");
  }

  private void validateSealableBlockHeader() {
    validateProcessableBlockHeader();
    checkState(this.ommersHash != null, "Missing ommers hash");
    checkState(this.stateRoot != null, "Missing state root");
    checkState(this.transactionsRoot != null, "Missing transaction root");
    checkState(this.receiptsRoot != null, "Missing receipts root");
    checkState(this.logsBloom != null, "Missing logs bloom filter");
    checkState(this.gasUsed > -1L, "Missing gas used");
    checkState(this.extraData != null, "Missing extra data field");
  }

  public BlockHeaderBuilder populateFrom(final ProcessableBlockHeader processableBlockHeader) {
    checkNotNull(processableBlockHeader);
    parentHash(processableBlockHeader.getParentHash());
    coinbase(processableBlockHeader.getCoinbase());
    difficulty(processableBlockHeader.getDifficulty());
    number(processableBlockHeader.getNumber());
    gasLimit(processableBlockHeader.getGasLimit());
    timestamp(processableBlockHeader.getTimestamp());
    return this;
  }

  public BlockHeaderBuilder populateFrom(final SealableBlockHeader sealableBlockHeader) {
    checkNotNull(sealableBlockHeader);
    parentHash(sealableBlockHeader.getParentHash());
    ommersHash(sealableBlockHeader.getOmmersHash());
    coinbase(sealableBlockHeader.getCoinbase());
    stateRoot(sealableBlockHeader.getStateRoot());
    transactionsRoot(sealableBlockHeader.getTransactionsRoot());
    receiptsRoot(sealableBlockHeader.getReceiptsRoot());
    logsBloom(sealableBlockHeader.getLogsBloom());
    difficulty(sealableBlockHeader.getDifficulty());
    number(sealableBlockHeader.getNumber());
    gasLimit(sealableBlockHeader.getGasLimit());
    gasUsed(sealableBlockHeader.getGasUsed());
    timestamp(sealableBlockHeader.getTimestamp());
    extraData(sealableBlockHeader.getExtraData());
    return this;
  }

  public BlockHeaderBuilder parentHash(final Hash hash) {
    checkNotNull(hash);
    this.parentHash = hash;
    return this;
  }

  public BlockHeaderBuilder ommersHash(final Hash hash) {
    checkNotNull(hash);
    this.ommersHash = hash;
    return this;
  }

  public BlockHeaderBuilder coinbase(final Address address) {
    checkNotNull(address);
    this.coinbase = address;
    return this;
  }

  public BlockHeaderBuilder stateRoot(final Hash hash) {
    checkNotNull(hash);
    this.stateRoot = hash;
    return this;
  }

  public BlockHeaderBuilder transactionsRoot(final Hash hash) {
    checkNotNull(hash);
    this.transactionsRoot = hash;
    return this;
  }

  public BlockHeaderBuilder receiptsRoot(final Hash hash) {
    checkNotNull(hash);
    this.receiptsRoot = hash;
    return this;
  }

  public BlockHeaderBuilder logsBloom(final LogsBloomFilter filter) {
    checkNotNull(filter);
    this.logsBloom = filter;
    return this;
  }

  public BlockHeaderBuilder difficulty(final UInt256 difficulty) {
    checkNotNull(difficulty);
    this.difficulty = difficulty;
    return this;
  }

  public BlockHeaderBuilder number(final long number) {
    checkArgument(number >= 0L);
    this.number = number;
    return this;
  }

  public BlockHeaderBuilder gasLimit(final long gasLimit) {
    checkArgument(gasLimit >= 0L);
    this.gasLimit = gasLimit;
    return this;
  }

  public BlockHeaderBuilder gasUsed(final long gasUsed) {
    checkArgument(gasUsed > -1L);
    this.gasUsed = gasUsed;
    return this;
  }

  public BlockHeaderBuilder timestamp(final long timestamp) {
    checkArgument(timestamp >= 0);
    this.timestamp = timestamp;
    return this;
  }

  public BlockHeaderBuilder extraData(final BytesValue data) {
    checkNotNull(data);

    this.extraData = data;
    return this;
  }

  public BlockHeaderBuilder mixHash(final Hash mixHash) {
    checkNotNull(mixHash);
    this.mixHash = mixHash;
    return this;
  }

  public BlockHeaderBuilder nonce(final long nonce) {
    this.nonce = OptionalLong.of(nonce);
    return this;
  }

  public BlockHeaderBuilder blockHashFunction(final BlockHashFunction blockHashFunction) {
    this.blockHashFunction = blockHashFunction;
    return this;
  }
}
