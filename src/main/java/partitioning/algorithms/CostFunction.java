package partitioning.algorithms;

public class CostFunction {
    private final double lengthWeight;
    private final double balanceWeight;

    public CostFunction(double lengthWeight, double balanceWeight) {
        this.lengthWeight = lengthWeight;
        this.balanceWeight = balanceWeight;
    }

    public double calculateCost(double length, double weight1, double totalWeight) {
        return lengthWeight * length + balanceWeight * Math.abs(weight1 - totalWeight / 2.0);
    }
}
