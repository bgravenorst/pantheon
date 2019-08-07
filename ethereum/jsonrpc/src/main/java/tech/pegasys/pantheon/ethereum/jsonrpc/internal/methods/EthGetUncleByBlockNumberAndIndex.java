/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.UnsignedIntParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.BlockResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.UncleBlockResult;

public class EthGetUncleByBlockNumberAndIndex extends AbstractBlockParameterMethod {

  public EthGetUncleByBlockNumberAndIndex(
      final BlockchainQueries blockchain, final JsonRpcParameter parameters) {
    super(blockchain, parameters);
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_UNCLE_BY_BLOCK_NUMBER_AND_INDEX.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequest request) {
    return getParameters().required(request.getParams(), 0, BlockParameter.class);
  }

  @Override
  protected BlockResult resultByBlockNumber(final JsonRpcRequest request, final long blockNumber) {
    final int index =
        getParameters().required(request.getParams(), 1, UnsignedIntParameter.class).getValue();
    return getBlockchainQueries()
        .getOmmer(blockNumber, index)
        .map(UncleBlockResult::build)
        .orElse(null);
  }
}
