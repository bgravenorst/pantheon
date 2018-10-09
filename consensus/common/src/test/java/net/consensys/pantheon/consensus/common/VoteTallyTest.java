package net.consensys.pantheon.consensus.common;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.pantheon.ethereum.core.Address;

import org.junit.Test;

public class VoteTallyTest {

  private static final Address validator1 =
      Address.fromHexString("000d836201318ec6899a67540690382780743280");
  private static final Address validator2 =
      Address.fromHexString("001762430ea9c3a26e5749afdb70da5f78ddbb8c");
  private static final Address validator3 =
      Address.fromHexString("001d14804b399c6ef80e64576f657660804fec0b");
  private static final Address validator4 =
      Address.fromHexString("0032403587947b9f15622a68d104d54d33dbd1cd");
  private static final Address validator5 =
      Address.fromHexString("00497e92cdc0e0b963d752b2296acb87da828b24");

  @Test
  public void validatorsAreNotAddedBeforeRequiredVoteCountReached() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void validatorAddedToListWhenMoreThanHalfOfProposersVoteToAdd() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);
  }

  @Test
  public void validatorsAreAddedInCorrectOrder() {
    final VoteTally voteTally =
        new VoteTally(asList(validator1, validator2, validator3, validator5));
    voteTally.addVote(validator1, validator4, VoteType.ADD);
    voteTally.addVote(validator2, validator4, VoteType.ADD);
    voteTally.addVote(validator3, validator4, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);
  }

  @Test
  public void duplicateVotesFromSameProposerAreIgnored() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void proposerChangingAddVoteToDropBeforeLimitReachedDiscardsAddVote() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator1, validator5, VoteType.DROP);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void proposerChangingAddVoteToDropAfterLimitReachedPreservesAddVote() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);
    voteTally.addVote(validator1, validator5, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);
  }

  @Test
  public void clearVotesAboutAValidatorWhenItIsAdded() {
    final VoteTally voteTally = fourValidators();
    // Vote to add validator5
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);

    // Then vote it back out
    voteTally.addVote(validator2, validator5, VoteType.DROP);
    voteTally.addVote(validator3, validator5, VoteType.DROP);
    voteTally.addVote(validator4, validator5, VoteType.DROP);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);

    // And then start voting to add it back in, but validator1's vote should have been discarded
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void requiresASingleVoteWhenThereIsOnlyOneValidator() {
    final VoteTally voteTally = new VoteTally(singletonList(validator1));
    voteTally.addVote(validator1, validator2, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators()).containsExactly(validator1, validator2);
  }

  @Test
  public void requiresTwoVotesWhenThereAreTwoValidators() {
    final VoteTally voteTally = new VoteTally(asList(validator1, validator2));
    voteTally.addVote(validator1, validator3, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators()).containsExactly(validator1, validator2);

    voteTally.addVote(validator2, validator3, VoteType.ADD);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3);
  }

  @Test
  public void resetVotes() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.discardOutstandingVotes();
    voteTally.addVote(validator3, validator5, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void validatorsAreNotRemovedBeforeRequiredVoteCountReached() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void validatorRemovedFromListWhenMoreThanHalfOfProposersVoteToDrop() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);
    voteTally.addVote(validator3, validator4, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3);
  }

  @Test
  public void validatorsAreInCorrectOrderAfterRemoval() {
    final VoteTally voteTally = new VoteTally(asList(validator1, validator2, validator4));
    voteTally.addVote(validator1, validator3, VoteType.DROP);
    voteTally.addVote(validator2, validator3, VoteType.DROP);
    voteTally.addVote(validator4, validator3, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator4);
  }

  @Test
  public void duplicateDropVotesFromSameProposerAreIgnored() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void proposerChangingDropVoteToAddBeforeLimitReachedDiscardsDropVote() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator1, validator4, VoteType.ADD);
    voteTally.addVote(validator2, validator4, VoteType.DROP);
    voteTally.addVote(validator3, validator4, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);
  }

  @Test
  public void proposerChangingDropVoteToAddAfterLimitReachedPreservesDropVote() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);
    voteTally.addVote(validator3, validator4, VoteType.DROP);
    voteTally.addVote(validator1, validator4, VoteType.ADD);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3);
  }

  @Test
  public void removedValidatorsVotesAreDiscarded() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator4, validator5, VoteType.ADD);
    voteTally.addVote(validator4, validator3, VoteType.DROP);

    voteTally.addVote(validator1, validator4, VoteType.DROP);
    voteTally.addVote(validator2, validator4, VoteType.DROP);
    voteTally.addVote(validator3, validator4, VoteType.DROP);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3);

    // Now adding only requires 2 votes (>50% of the 3 remaining validators)
    // but validator4's vote no longer counts
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator1, validator3, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3);
  }

  @Test
  public void clearVotesAboutAValidatorWhenItIsDropped() {
    final VoteTally voteTally =
        new VoteTally(asList(validator1, validator2, validator3, validator4, validator5));
    // Vote to remove validator5
    voteTally.addVote(validator1, validator5, VoteType.DROP);
    voteTally.addVote(validator2, validator5, VoteType.DROP);
    voteTally.addVote(validator3, validator5, VoteType.DROP);

    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);

    // Then vote it back in
    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator5, VoteType.ADD);
    voteTally.addVote(validator4, validator5, VoteType.ADD);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);

    // And then start voting to drop it again, but validator1's vote should have been discarded
    voteTally.addVote(validator2, validator5, VoteType.DROP);
    voteTally.addVote(validator3, validator5, VoteType.DROP);
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);
  }

  @Test
  public void trackMultipleOngoingVotesIndependently() {
    final VoteTally voteTally = fourValidators();
    voteTally.addVote(validator1, validator5, VoteType.ADD);
    voteTally.addVote(validator1, validator3, VoteType.DROP);

    voteTally.addVote(validator2, validator5, VoteType.ADD);
    voteTally.addVote(validator2, validator1, VoteType.DROP);

    // Neither vote has enough votes to complete.
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4);

    voteTally.addVote(validator3, validator5, VoteType.ADD);
    voteTally.addVote(validator3, validator1, VoteType.DROP);

    // Validator 5 now has 3 votes and is added
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator1, validator2, validator3, validator4, validator5);

    voteTally.addVote(validator4, validator5, VoteType.ADD);
    voteTally.addVote(validator4, validator1, VoteType.DROP);

    // Validator 1 now gets dropped.
    assertThat(voteTally.getCurrentValidators())
        .containsExactly(validator2, validator3, validator4, validator5);
  }

  private VoteTally fourValidators() {
    return new VoteTally(asList(validator1, validator2, validator3, validator4));
  }
}
