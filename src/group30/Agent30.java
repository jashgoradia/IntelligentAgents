package group30;

import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent30 extends AbstractNegotiationParty
{
    private double MINIMUM_TARGET = 0.8;
    private Bid lastOffer;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info)
    {
        super.init(info);

        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();

        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

            // Assuming that issues are discrete only
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                System.out.println(valueDiscrete.getValue());
                System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
                try {
                    System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        MINIMUM_TARGET = (utilitySpace.getUtility(getMaxUtilityBid()) + utilitySpace.getUtility(getMinUtilityBid()))/2;
    }

    /**
     * Makes a random offer above the minimum utility target
     * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise.
     * We can do one of three things:
     *  -> Accept the opponent's offer
     *  -> Generate a counter-offer
     *  -> End the negotation
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions)
    {

        double offerUtility = utilitySpace.getUtility(getMaxUtilityBid());
        double discountFactor = (1 - timeline.getTime());
        double minimumUtilityThreshold = discountFactor * offerUtility;
        double theirOfferUtility = utilitySpace.getUtility(lastOffer) * discountFactor;
        if (minimumUtilityThreshold < MINIMUM_TARGET) {
            System.err.printf("Minimum util is lower than target -> %f < %f\n", minimumUtilityThreshold, MINIMUM_TARGET);
        }

        // Check for acceptance if we have received an offer
        if (lastOffer != null)
            if (timeline.getTime() >= 0.99)
                if (getUtility(lastOffer) >= MINIMUM_TARGET)
                    return new Accept(getPartyId(), lastOffer);
                else
                    return new EndNegotiation(getPartyId());

        //First check the offer -> see if we can accept
        if (theirOfferUtility > MINIMUM_TARGET) {
            return new Accept(getPartyId(), lastOffer);
        }
        //IF above fails, THEN make new offer or end negotiation
        if (timeline.getTime() < 0.99){
            return new Offer(getPartyId(), generateRandomBidAboveTarget(minimumUtilityThreshold));
        } else {
            return new EndNegotiation(getPartyId());
        }
        // Otherwise, send out a random offer above the target utility

    }

    private Bid getMaxUtilityBid() {
        try {
            return utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bid getMinUtilityBid() {
        try {
            return utilitySpace.getMinUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bid generateRandomBidAboveTarget(double target)
    {
        Bid randomBid;
        double util;
        int i = 0;
        // try 100 times to find a bid under the target utility
        do
        {
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
        }
        while (util < target && i++ < 100);
        return randomBid;
    }

    /**
     * Remembers the offers received by the opponent.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action)
    {
        if (action instanceof Offer)
        {
            lastOffer = ((Offer) action).getBid();
        }
    }

    @Override
    public String getDescription()
    {
        return "Practice Submission";
    }

    /**
     * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace()
    {
        return super.estimateUtilitySpace();
    }

}

