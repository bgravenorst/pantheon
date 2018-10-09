package net.consensys.pantheon.consensus.clique.jsonrpc.methods;

import static net.consensys.pantheon.consensus.clique.CliqueHelpers.getValidatorsOfBlock;

import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import net.consensys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.util.Optional;

public class CliqueGetSignersAtHash implements JsonRpcMethod {
  public static final String CLIQUE_GET_SIGNERS_AT_HASH = "clique_getSignersAtHash";
  private final BlockchainQueries blockchainQueries;
  private final JsonRpcParameter parameters;

  public CliqueGetSignersAtHash(
      final BlockchainQueries blockchainQueries, final JsonRpcParameter parameter) {
    this.blockchainQueries = blockchainQueries;
    this.parameters = parameter;
  }

  @Override
  public String getName() {
    return CLIQUE_GET_SIGNERS_AT_HASH;
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Optional<BlockHeader> blockHeader = blockHeader(request);
    return blockHeader
        .<JsonRpcResponse>map(
            bh -> new JsonRpcSuccessResponse(request.getId(), getValidatorsOfBlock(bh)))
        .orElse(new JsonRpcErrorResponse(request.getId(), JsonRpcError.INTERNAL_ERROR));
  }

  private Optional<BlockHeader> blockHeader(final JsonRpcRequest request) {
    final Hash hash = parameters.required(request.getParams(), 0, Hash.class);
    return blockchainQueries.blockByHash(hash).map(BlockWithMetadata::getHeader);
  }
}
