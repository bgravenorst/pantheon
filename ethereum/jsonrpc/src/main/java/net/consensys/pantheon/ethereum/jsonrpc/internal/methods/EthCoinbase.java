package net.consensys.pantheon.ethereum.jsonrpc.internal.methods;

import net.consensys.pantheon.ethereum.blockcreation.MiningCoordinator;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.util.Optional;

public class EthCoinbase implements JsonRpcMethod {

  private final MiningCoordinator miningCoordinator;

  public EthCoinbase(final MiningCoordinator miningCoordinator) {
    this.miningCoordinator = miningCoordinator;
  }

  @Override
  public String getName() {
    return "eth_coinbase";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    final Optional<Address> coinbase = miningCoordinator.getCoinbase();
    if (coinbase.isPresent()) {
      return new JsonRpcSuccessResponse(req.getId(), coinbase.get().toString());
    }
    return new JsonRpcErrorResponse(req.getId(), JsonRpcError.COINBASE_NOT_SPECIFIED);
  }
}
