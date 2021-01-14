package group30;

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
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent30 extends AbstractNegotiationParty {
    private double MINIMUM_TARGET = 0.8;
    private Bid lastOffer;
    private Map<Integer, Map<String, Integer>> opponentBids;
    private Map<Integer, Map<String, Double>> opponentValues;

    private Map<String, Integer> optionCounts;
    private List<Issue> issues;
    private List<Bid> bids;
    private Set<Bid> desperateBids;
    private int bidCount = 0;
    private double sumOfUnnormalizedWeightOfIssues;
    private Map<Bid, Double> opponentsOffers;

    private static final boolean VERBOSE = false;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);

//        File file = new File("myLogs.txt");
//        BufferedWriter br = null;
//        try {
//            br = new BufferedWriter(new FileWriter(file));
//        } catch (IOException e) {
//            System.exit(-1);
//        }
//
//        BufferedWriter finalBr = br;
//        Logger.getLogger("tournamentRun").addHandler(new Handler() {
//            @Override
//            public void publish(LogRecord record) {
//                try {
//                    finalBr.write(record.getMessage());
//                    finalBr.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void flush() {
//                try {
//                    finalBr.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void close() throws SecurityException {
//                try {
//                    finalBr.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        if (hasPreferenceUncertainty()) {
            System.out.println("Preference uncertainty is enabled.");
        }



        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        this.issues = additiveUtilitySpace.getDomain().getIssues();

        optionCounts = new HashMap<>();
        for (Issue issue : issues) {
            IssueDiscrete discrete = (IssueDiscrete) issue;
            optionCounts.put(discrete.getName(), discrete.getNumberOfValues());
        }


        this.bids = new ArrayList<>();
        this.desperateBids = new HashSet<>();
        this.opponentBids = new TreeMap<>();
        this.opponentsOffers = new HashMap<>();

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
//        MINIMUM_TARGET = (utilitySpace.getUtility(getMaxUtilityBid()) + utilitySpace.getUtility(getMinUtilityBid())) / 2;
//        System.out.printf("Minimum target: %f\n", MINIMUM_TARGET);
        System.out.println("v1.6");
    }

    /**
     * Makes a random offer above the minimum utility target
     * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise.
     * We can do one of three things:
     * -> Accept the opponent's offer
     * -> Generate a counter-offer
     * -> End the negotation
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        if (lastOffer == null){
            Bid initialBid = generateRandomBidAboveTarget(MINIMUM_TARGET);
            System.out.println("First Bid: Our utility = " + utilitySpace.getUtility(initialBid));
            bids.add(initialBid);
            Logger.getLogger("tournamentRun").log(Level.INFO, initialBid.toString() + " -> " + utilitySpace.getUtility(initialBid));
            return new Offer(getPartyId(), initialBid);
        }

        double offerUtility = utilitySpace.getUtility(getMaxUtilityBid());
//        double discountFactor = (1 - timeline.getTime());
        //double minimumUtilityThreshold = Math.min(discountFactor * offerUtility + MINIMUM_TARGET, 1.0);
        double minimumUtilityThreshold = computeNashBargainingSolution();//MINIMUM_TARGET;//interpolate(MINIMUM_TARGET, offerUtility, timeline.getTime());
        double theirOfferUtility = utilitySpace.getUtility(lastOffer);
        double opponentUtlity = predictOpponentUtility(lastOffer);
        if (VERBOSE) {
            System.out.println("The predicted opponent utility is: "+ opponentUtlity);
            System.out.printf("==============\nTheir offer utility: %f\nOur min util: %f\n",theirOfferUtility, minimumUtilityThreshold);
        }
        opponentsOffers.put(lastOffer,utilitySpace.getUtility(lastOffer));
        System.out.println(timeline.getTime());
        // Check for acceptance if we have received an offer
//        if (lastOffer != null)
        // TODO If we get past a certain number of rounds, look for bids we have made where our utility was higher than minimum_target and their predicted utility is highest
        if (timeline.getTime() >= 0.95 && timeline.getTime() < 0.97) {
            if (theirOfferUtility > minimumUtilityThreshold) {
                System.out.println("Accept at good offer!");
                return new Accept(getPartyId(), lastOffer);
            }
            double maxOpponentUtility = 0.0;
            Bid bidToMake = null;
            for (Bid bid : bids){
                if (utilitySpace.getUtility(bid) >= MINIMUM_TARGET){
                    double opponentUtility = predictOpponentUtility(bid);
                    if (opponentUtility  > maxOpponentUtility){
                        bidToMake = bid;
                        maxOpponentUtility = opponentUtility;
                    }
                }
            }
            Logger.getLogger("tournamentRun").log(Level.INFO, bidToMake.toString() + " -> " + utilitySpace.getUtility(bidToMake));
            return new Offer(getPartyId(), bidToMake);
        }
        else if (timeline.getTime() >= 0.97 && timeline.getTime() < 0.995) {
            if (getUtility(lastOffer) >= MINIMUM_TARGET * 0.9) {
                System.err.println("Accept at final time");
                return new Accept(getPartyId(), lastOffer);
            } else {
                // Towards the last few rounds, bid something they have offered where our utility is the highest

                //LOGIC 1
                Map<Bid, Double> offerSums = new HashMap<>();
                for (Bid bid : opponentsOffers.keySet()) {
                    offerSums.put(bid, Math.min(opponentsOffers.get(bid), predictOpponentUtility(bid)));
                }
                for (Bid bid : bids) {
                    offerSums.put(bid, Math.min(utilitySpace.getUtility(bid), predictOpponentUtility(bid)));
                }

                Bid highestMinBid = null;
                double highestMinUtil = 0.0;

                for (Bid bid : offerSums.keySet()) {
                    double combinedUtil = offerSums.get(bid);
                    if (combinedUtil > highestMinUtil && !desperateBids.contains(bid)) {
                        highestMinBid = bid;
                        highestMinUtil = combinedUtil;
                    }
                }
                if (highestMinBid == null)
                    System.err.println("oups");
                this.desperateBids.add(highestMinBid);
                printBid(highestMinBid);
                printBid(lastOffer);
                System.out.println("Return highest seen combined bid");
                Logger.getLogger("tournamentRun").log(Level.INFO, highestMinBid.toString() + " -> " + utilitySpace.getUtility(highestMinBid));
                return new Offer(getPartyId(), highestMinBid);

                //LOGIC 2
//                Bid hightestOpponentUtilityBid = generateRandomBidAboveTarget(MINIMUM_TARGET);
//                double maxUtilInMap = (Collections.max(opponentsOffers.values()));
//                if (maxUtilInMap < MINIMUM_TARGET) {
//                    for (Map.Entry<Bid, Double> entry : opponentsOffers.entrySet()) {
//                        if (entry.getValue() == maxUtilInMap) {
//                            hightestOpponentUtilityBid = entry.getKey();
//                        }
//                    }
//                    System.out.println("Couldn't find an offer with utility > target \n Utility of our bid: " + utilitySpace.getUtility(hightestOpponentUtilityBid));
//                    opponentsOffers.remove(hightestOpponentUtilityBid);
//                    return new Offer(getPartyId(), hightestOpponentUtilityBid);
//                } else {
//                    System.out.println("Seen offer than we should have accepted earlier");
//                }

//            else {
//                System.err.println("End in first part duplicated");
//                return new EndNegotiation(getPartyId());
//            }
            }
        } else if (timeline.getTime() >= 0.995) {
            System.out.print("Final round: ");
            if (getUtility(lastOffer) >= utilitySpace.getReservationValue()) {
                System.out.println("accept");
                return new Accept(getPartyId(), lastOffer);
            } else {
                System.out.println("reject");
                return new EndNegotiation(getPartyId());
            }

        }



        //First check the offer -> see if we can accept
        if (theirOfferUtility > minimumUtilityThreshold) {
            System.out.println("Accept at good offer!");
            return new Accept(getPartyId(), lastOffer);
        }
        //IF above fails, THEN make new offer or end negotiation
        if (timeline.getTime() < 0.99) {
            //Bid bid = generateRandomBidAboveTarget(minimumUtilityThreshold);
            List<Bid> randomBids = generateRandomBidsAboveTarget(10, minimumUtilityThreshold);
            randomBids = sortBidsPerOpponentModel(randomBids);
            Bid bid = randomBids.get(0);

            //Elicit the user (possibly)
            System.out.println(user.getElicitationCost() + " / " + user.getTotalBother());
            if (false && user.getElicitationCost() < 0.01 && user.getTotalBother() < 0.15) {
                userModel = user.elicitRank(bid, userModel);
                estimateUtilitySpace();
            }
            double bidUtility = utilitySpace.getUtility(bid);
            //System.out.println("Making an offer with utility: " + bidUtility);
//            if (bidUtility < MINIMUM_TARGET && theirOfferUtility > bidUtility) {
//                return new Accept(getPartyId(), lastOffer);
//            }
            bids.add(bid);
            return new Offer(getPartyId(), bid);
        } else {
            System.err.println("End in second part duplicated");
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

    private double computeNashBargainingSolution() {
        double highestNashPoint = MINIMUM_TARGET;
        Bid highestNashBid = null;
        for (Bid bid : bids) {
            double ourOutcome = utilitySpace.getUtility(bid);
            double theirOutcome = predictOpponentUtility(bid);
            //TODO this is what we play with - may or may not need to include the reservation value
            double nashPoint = ourOutcome * theirOutcome;

            if (nashPoint > highestNashPoint) {
                highestNashPoint = nashPoint;
                highestNashBid = bid;
            }
        }

        return highestNashPoint;

    }

    private void printBid(Bid bid) {
        System.out.println(bid);
        System.out.printf("Our util: %f\nTheir util: %f\n", utilitySpace.getUtility(bid), predictOpponentUtility(bid));
    }

    private Bid generateRandomBidAboveTarget(double target) {
        Map<Bid,Double>randomBids = new HashMap<>();
        Bid randomBid;
        double util;
        int i = 0;
        // try 100 times to find a bid under the target utility
        do {
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
            randomBids.put(randomBid,util);
        }
        while (util < target && i++ < 100);
        if (util<target) {
            //System.out.println("Random util was: " + utilitySpace.getUtility(randomBid));
            double maxUtilInMap = (Collections.max(randomBids.values()));
            for (Map.Entry<Bid, Double> entry : randomBids.entrySet()) {
                if (entry.getValue() == maxUtilInMap) {
                    randomBid = entry.getKey();
                }
            }
            //System.out.println("Tried finding the bid with highest util, new util: " + utilitySpace.getUtility(randomBid));
            return randomBid;
        }
        return randomBid;
    }

    private List<Bid> generateRandomBidsAboveTarget(int n, double target) {
        List<Bid> bids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            bids.add(generateRandomBidAboveTarget(target));
        }
        return bids;
    }

    private List<Bid> sortBidsPerOpponentModel(List<Bid> bids) {
        Collections.sort(bids, (bid1, bid2) -> {
            double util1 = predictOpponentUtility(bid1);
            double util2 = predictOpponentUtility(bid2);
            if (util1 > util2) {
                return -1;
            } else if (util1 < util2) {
                return 1;
            }
            return 0;
        });
        return bids;
    }

    private void updateOpponentCounts(Bid opponentBid) {
        for (Issue issue : opponentBid.getIssues()) {
            int issueNumber = issue.getNumber();
            String chosenOption = ((ValueDiscrete) opponentBid.getValue(issueNumber)).getValue();
            Map<String, Integer> currentIssueCounts = opponentBids.getOrDefault(issueNumber, new TreeMap<>());
            currentIssueCounts.put(chosenOption, currentIssueCounts.getOrDefault(chosenOption, 0) + 1);
            opponentBids.put(issueNumber, currentIssueCounts);
        }
        sumOfUnnormalizedWeightOfIssues = computeSumOfUnnormalizedWeightOfIssues();
        //displayOpponentCounts();
    }

    private int getOptionRank(Map<String, Integer> optionsForIssue, Bid opponentBid, int issueNumber) {
        LinkedHashMap<String, Integer> sorted = optionsForIssue.entrySet().stream().
                sorted(Map.Entry.comparingByValue()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        List<String> keys = new ArrayList<>(sorted.keySet());
        int index = keys.indexOf(((ValueDiscrete) opponentBid.getValue(issueNumber)).getValue());
        return sorted.size() - index;
    }

    private double computeVo(int issueRank, int issueCount) {
        return (issueCount - issueRank + 1.0) / (issueCount);
    }

    private double computeUnnormalizedWeightOfIssue(Issue issue) {
        double sum = 0;
        final double tsqrd = bidCount * bidCount;
        IssueDiscrete discrete = (IssueDiscrete) issue;
        Map<String, Integer> optionFrequencies = opponentBids.get(issue.getNumber());
        for (ValueDiscrete option: discrete.getValues()) {
            sum += Math.pow(optionFrequencies.getOrDefault(option.getValue(), 0), 2) / (tsqrd);
        }
        return sum;
    }

    private double computeNormalizedWeightOfIssue(double sumOfUnnormalizedWeightOfIssues, double unnormalizedWeightOfIssue) {
        return  unnormalizedWeightOfIssue / sumOfUnnormalizedWeightOfIssues;
    }

    private double computeSumOfUnnormalizedWeightOfIssues(){
        double sum = 0;
        for (Issue issue: issues){
            sum += computeUnnormalizedWeightOfIssue(issue);
        }
        return sum;
    }

    private double predictOpponentUtility(Bid opponentBid) {
        double opponentUtility = 0;
        for (Issue issue : opponentBid.getIssues()) {
            int issueNumber = issue.getNumber();
            Map<String, Integer> optionsForIssue = opponentBids.get(issueNumber);
            int rank = getOptionRank(optionsForIssue, opponentBid, issueNumber);
            double vo = computeVo(rank, optionCounts.get(((IssueDiscrete) issue).getName()));
            opponentUtility += (vo * computeNormalizedWeightOfIssue(sumOfUnnormalizedWeightOfIssues, computeUnnormalizedWeightOfIssue(issue)));
        }
        return opponentUtility;
    }

    private void displayOpponentCounts() {
        for (int issueNumber : opponentBids.keySet()) {
            System.out.print(issueNumber + ": ");
            for (String optionNumber : opponentBids.get(issueNumber).keySet()) {
                System.out.print(optionNumber + ": " + opponentBids.get(issueNumber).get(optionNumber) + "\t");
            }
            System.out.println();
        }
    }

    /**
     * Remembers the offers received by the opponent.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action) {
        if (action instanceof Offer) {
            Logger.getLogger("tournamentRun").log(Level.INFO, action.toString() + " -> " + utilitySpace.getUtility(((Offer) action).getBid()));
            lastOffer = ((Offer) action).getBid();
            bidCount++;
            updateOpponentCounts(lastOffer);
        }
    }

    @Override
    public String getDescription() {
        return "Group 30 Submission";
    }

    /**
     * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace() {
        System.out.println("ESTIMATED");
        AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(getDomain());
        BidRanking bidRanking = getUserModel().getBidRanking();
        factory.estimateUsingBidRanks(bidRanking);
        return factory.getUtilitySpace();
    }

}

