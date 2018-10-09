package net.consensys.pantheon.ethereum.mainnet;

import net.consensys.pantheon.ethereum.core.Gas;

public class HomesteadGasCalculator extends FrontierGasCalculator {

  private static final Gas TX_CREATE_EXTRA = Gas.of(32_000L);

  @Override
  protected Gas txCreateExtraGasCost() {
    return TX_CREATE_EXTRA;
  }
}
