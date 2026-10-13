package partitioning.algorithms;

public class CostFunction {
    private final double lengthWeight;
    private final double balanceWeight;
    private final double maxWeight;
    private final double totalWeight;
    private final double overhead;
    private final double minRelPartSize;

    public CostFunction(double lengthWeight, double balanceWeight, double maxWeight, double totalWeight, double minRelPartSize ) {
        this.lengthWeight = lengthWeight;
        this.balanceWeight = balanceWeight;
        this.maxWeight = maxWeight;
        this.totalWeight = totalWeight;
        this.overhead = totalWeight - Math.floor(totalWeight/maxWeight) * maxWeight;
        this.minRelPartSize = minRelPartSize;
    }

    public double calculateCost(double length, double leftWeight) {
        double rightWeight = totalWeight - leftWeight;
        int regionsLeftComplete = (int) Math.floor(leftWeight/maxWeight);
        int regionsRightComplete = (int) Math.floor(rightWeight/maxWeight);
        // int regionsComplete = (int) Math.floor(totalWeight/maxWeight);

        double leftExceed = leftWeight - regionsLeftComplete * maxWeight;
        double rightExceed = rightWeight - regionsRightComplete * maxWeight;
        boolean leftToOne = regionsLeftComplete == 0 && regionsRightComplete != 0;
        boolean rightToOne = regionsRightComplete == 0 && regionsLeftComplete != 0;
        int extraRegion =  (overhead != 0 && leftExceed + rightExceed <= maxWeight + 0.9*overhead) || (overhead == 0 && leftExceed + rightExceed > 0.) ? 1 : 0;
        double oneRegion = (leftToOne ? (maxWeight - leftExceed) : 0) + (rightToOne ? (maxWeight - rightExceed) : 0);  // in case of one region last step as much as possible should be pushed to that region
        double manyRegion = !(leftToOne || rightToOne) ? 0 : Math.abs(rightExceed - leftExceed);// in case no region is finished one should balance exceeds
        double localBalance = (oneRegion + manyRegion)/maxWeight;
        double penInit = (leftWeight < minRelPartSize * totalWeight || rightWeight < minRelPartSize * totalWeight) ? 1 :0; // exist global cut disbalance
        return lengthWeight * length 
               + balanceWeight * Math.abs(leftWeight/totalWeight - 0.5) 
               + 100*extraRegion 
               + localBalance 
               + 10*penInit;
    }
}
