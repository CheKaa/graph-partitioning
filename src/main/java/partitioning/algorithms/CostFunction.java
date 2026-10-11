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
        this.overhead = totalWeight/maxWeight - Math.floor(totalWeight/maxWeight);
        this.minRelPartSize = minRelPartSize;
    }

    public double calculateCost(double length, double leftWeight) {
        double rightWeight = totalWeight - leftWeight;
        double leftRegionBound = leftWeight/maxWeight;
        double rightRegionBound = rightWeight/maxWeight;
        int regionsLeftComplete = (int) Math.floor(leftRegionBound);
        int regionsRightComplete = (int) Math.floor(rightRegionBound);
        // int regionsComplete = (int) Math.floor(totalWeight/maxWeight);

        double leftExceedRate = leftRegionBound - regionsLeftComplete;
        double rightExceedRate = rightRegionBound - regionsRightComplete;
        boolean leftToOne = regionsLeftComplete == 0 && regionsRightComplete != 0;
        boolean rightToOne = regionsRightComplete == 0 && regionsLeftComplete != 0;
        int extraRegion =  (leftExceedRate + rightExceedRate >= 1 + 0.99*overhead) || ((leftExceedRate + rightExceedRate) == 0) ? 0 : 1;
        double oneRegion = (leftToOne ? (1 - leftRegionBound) : 0) + (rightToOne ? (1 - rightRegionBound) : 0);
        double manyRegion = (leftToOne || rightToOne) ? 0 : (Math.abs(overhead/2 - leftExceedRate) + Math.abs(overhead - rightExceedRate));
        double penInit = (leftWeight < minRelPartSize * totalWeight || rightWeight < minRelPartSize * totalWeight) ? 1 :0;
        return lengthWeight * length 
               + balanceWeight * Math.abs(leftWeight/totalWeight - 0.5) 
               + extraRegion 
               + oneRegion 
               + 10*penInit
               + manyRegion;
    }
}
