package net.consensys.pantheon.tests.acceptance.dsl.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.pantheon.tests.acceptance.dsl.node.PantheonNode;

import java.util.List;

import io.vertx.core.Vertx;

public class WebSocket {

  private static final String HEX_PATTERN = "0x[0-9a-f]+";

  private final WebSocketConnection connection;

  public WebSocket(final Vertx vertx, final PantheonNode node) {
    this.connection = new WebSocketConnection(vertx, node);
  }

  public Subscription subscribe() {
    final JsonRpcSuccessEvent subscribe = connection.subscribe("newPendingTransactions");

    assertThat(subscribe).isNotNull();
    assertThat(subscribe.getVersion()).isEqualTo("2.0");
    assertThat(subscribe.getId()).isGreaterThan(0);
    assertThat(subscribe.getResult()).matches(HEX_PATTERN);

    return new Subscription(connection, subscribe.getResult());
  }

  public void unsubscribe(final Subscription subscription) {
    final JsonRpcSuccessEvent unsubscribe = connection.unsubscribe(subscription);

    assertThat(unsubscribe).isNotNull();
    assertThat(unsubscribe.getVersion()).isEqualTo("2.0");
    assertThat(unsubscribe.getId()).isGreaterThan(0);
    assertThat(unsubscribe.getResult()).isEqualTo("true");
  }

  public void verifyTotalEventsReceived(final int expectedTotalEventCount) {
    final List<SubscriptionEvent> events = connection.getSubscriptionEvents();
    assertThat(events).isNotNull();
    assertThat(events.size()).isEqualTo(expectedTotalEventCount);
  }
}
