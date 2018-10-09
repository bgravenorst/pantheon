package net.consensys.pantheon.ethereum.jsonrpc.internal.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import net.consensys.pantheon.ethereum.jsonrpc.internal.filter.FilterManager;
import net.consensys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.LogWithMetadata;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.results.LogsResult;
import net.consensys.pantheon.util.bytes.BytesValue;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EthGetFilterChangesTest {

  private EthGetFilterChanges method;

  @Mock FilterManager filterManager;

  @Before
  public void setUp() {
    method = new EthGetFilterChanges(filterManager, new JsonRpcParameter());
  }

  @Test
  public void shouldReturnCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("eth_getFilterChanges");
  }

  @Test
  public void shouldThrowExceptionWhenNoParamsSupplied() {
    final JsonRpcRequest request = requestWithParams();

    final Throwable thrown = catchThrowable(() -> method.response(request));
    assertThat(thrown)
        .hasNoCause()
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Missing required json rpc parameter at index 0");

    verifyZeroInteractions(filterManager);
  }

  @Test
  public void shouldThrowExceptionWhenInvalidParamsSupplied() {
    final JsonRpcRequest request = requestWithParams();

    final Throwable thrown = catchThrowable(() -> method.response(request));
    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Missing required json rpc parameter at index 0");

    verifyZeroInteractions(filterManager);
  }

  @Test
  public void shouldReturnErrorResponseWhenFilterManagerDoesNotFindAnyFilters() {
    final JsonRpcRequest request = requestWithParams("0x1");
    final JsonRpcResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.FILTER_NOT_FOUND);

    when(filterManager.blockChanges(anyString())).thenReturn(null);
    when(filterManager.pendingTransactionChanges(anyString())).thenReturn(null);
    when(filterManager.logsChanges(anyString())).thenReturn(null);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
    verify(filterManager).blockChanges(eq("0x1"));
    verify(filterManager).pendingTransactionChanges(eq("0x1"));
    verify(filterManager).logsChanges(eq("0x1"));
  }

  @Test
  public void shouldReturnHashesWhenFilterManagerFindsBlockFilterWithHashes() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges("0x1")).thenReturn(Lists.newArrayList(Hash.ZERO));

    final List<String> expectedHashes =
        Lists.newArrayList(Hash.ZERO).stream().map(Hash::toString).collect(Collectors.toList());
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, expectedHashes);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void shouldReturnEmptyHashesWhenFilterManagerFindsBlockFilterWithNoChanges() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges("0x1")).thenReturn(Lists.newArrayList());

    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, Lists.newArrayList());

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void shouldReturnHashesWhenFilterManagerFindsPendingTransactionFilterWithHashes() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges(anyString())).thenReturn(null);
    when(filterManager.pendingTransactionChanges("0x1")).thenReturn(Lists.newArrayList(Hash.ZERO));

    final List<String> expectedHashes =
        Lists.newArrayList(Hash.ZERO).stream().map(Hash::toString).collect(Collectors.toList());
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, expectedHashes);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void shouldReturnEmptyHashesWhenFilterManagerFindsPendingTransactionFilterWithNoChanges() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges(anyString())).thenReturn(null);
    when(filterManager.pendingTransactionChanges("0x1")).thenReturn(Lists.newArrayList());

    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, Lists.newArrayList());

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void shouldReturnLogsWhenFilterManagerFindsLogFilterWithLogs() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges(anyString())).thenReturn(null);
    when(filterManager.pendingTransactionChanges(anyString())).thenReturn(null);
    when(filterManager.logsChanges("0x1")).thenReturn(Lists.newArrayList(logWithMetadata()));

    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, new LogsResult(Lists.newArrayList(logWithMetadata())));

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByFieldRecursively(expectedResponse);
  }

  @Test
  public void shouldReturnEmptyLogsWhenFilterManagerFindsLogFilterWithNoChanges() {
    final JsonRpcRequest request = requestWithParams("0x1");
    when(filterManager.blockChanges(anyString())).thenReturn(null);
    when(filterManager.pendingTransactionChanges(anyString())).thenReturn(null);
    when(filterManager.logsChanges("0x1")).thenReturn(Lists.newArrayList());

    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, new LogsResult(Lists.newArrayList()));

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByFieldRecursively(expectedResponse);
  }

  private JsonRpcRequest requestWithParams(final Object... params) {
    return new JsonRpcRequest("2.0", "eth_getFilterChanges", params);
  }

  private LogWithMetadata logWithMetadata() {
    return LogWithMetadata.create(
        0,
        100L,
        Hash.ZERO,
        Hash.ZERO,
        0,
        Address.fromHexString("0x0"),
        BytesValue.EMPTY,
        Lists.newArrayList(),
        false);
  }
}
