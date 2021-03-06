/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.tests.acceptance.dsl.transaction.perm;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.tests.acceptance.dsl.account.Account;
import tech.pegasys.pantheon.tests.acceptance.dsl.account.Accounts;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.Transaction;

public class AccountSmartContractPermissioningTransactions {

  private final Accounts accounts;

  public AccountSmartContractPermissioningTransactions(final Accounts accounts) {
    this.accounts = accounts;
  }

  public Transaction<Hash> allowAccount(final String contractAddress, final Account account) {
    return new AccountSmartContractPermissioningAllowAccountTransaction(
        accounts.getPrimaryBenefactor(),
        Address.fromHexString(contractAddress),
        Address.fromHexString(account.getAddress()));
  }

  public Transaction<Hash> forbidAccount(final String contractAddress, final Account account) {
    return new AccountSmartContractPermissioningForbidAccountTransaction(
        accounts.getPrimaryBenefactor(),
        Address.fromHexString(contractAddress),
        Address.fromHexString(account.getAddress()));
  }

  public Transaction<Boolean> isAccountAllowed(
      final String contractAddress, final Account account) {
    return new AccountSmartContractPermissioningIsAllowedTransaction(
        Address.fromHexString(contractAddress), Address.fromHexString(account.getAddress()));
  }
}
