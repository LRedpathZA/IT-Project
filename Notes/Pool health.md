# Pool Health Calculation Explained

The pool health system uses a weighted deviation model to calculate an overall health score (out of 10.0) based on how far each of our four core metrics—pH, Chlorine, Alkalinity, and Stabilizer—are from their ideal mid-point.

This calculation is performed within the `calculateOverallHealth(TestLogModel data)` method in our `PoolHealth.java` fragment.

## 1. Individual Metric Health (Deviation-Based)

The first step is to determine a raw health percentage (out of 100) for each metric. This is done by comparing the tested value to a defined Ideal Mid-point and a Maximum Sensible Deviation ($DEV$).

For a specific metric (e.g., pH):

1. **Find the Deviation**: Calculate the absolute difference between the tested value (`data.getPh()`) and the Ideal Mid-point (e.g., $7.5$).
2. **Normalize the Deviation**: Divide the Deviation by the metric's $DEV$ value. This gives a Deviation Percentage (capped at $1.0$ or $100\%$).

$$\text{Deviation Percent} = \min \left(1.0, \frac{|\text{Tested Value} - \text{Mid-point}|}{\text{Max Deviation (DEV)}}\right)$$

3. **Calculate Metric Health**: Subtract the Deviation Percentage from $1.0$ and multiply by 100.

$$\text{Metric Health Score} = 100 \times (1.0 - \text{Deviation Percent})$$

### Metric Parameters

| Metric | Ideal Mid-point | Max Deviation (DEV) | Rationale / Source |
|--------|-----------------|---------------------|-------------------|
| pH | 7.5 | 0.5 | Common industry target, deviation based on safe range (7.0 to 8.0). |
| Chlorine (FC) | 2.0 ppm | 3.0 ppm | Deviation is large to allow for $0.0$ up to $5.0$ ppm (shock level) without immediately hitting $0\%$ health. |
| Alkalinity (TA) | 100.0 ppm | 40.0 ppm | Based on a standard optimal range of $80-120$ ppm. |
| Stabilizer (CYA) | 40.0 ppm | 30.0 ppm | Based on a standard optimal range of $30-50$ ppm. |

## 2. Overall Health Score (Weighted Average)

Once all four individual health scores are calculated, they are combined into a single, overall score (out of 10.0) using a weighted average.

This assigns greater importance to the metrics that most immediately impact swimmer comfort, safety, and water balance (pH and Chlorine) and lesser importance to supporting metrics (Alkalinity and Stabilizer).

### Metric Weights

| Metric | Weight | Rationale |
|--------|--------|-----------|
| pH Health | 35% (0.35) | Critical for swimmer comfort, sanitizer effectiveness, and equipment life. |
| Chlorine Health | 35% (0.35) | Critical for sanitation and safety. |
| Alkalinity Health | 20% (0.20) | Important for buffering pH and stability. |
| Stabilizer Health | 10% (0.10) | Important for protecting chlorine from UV light. |

The final Overall Health Score (out of 10.0) is calculated as:

$$\text{Overall Score} = \frac{(\text{pH Health} \times 0.35) + (\text{Chl Health} \times 0.35) + (\text{Alk Health} \times 0.20) + (\text{Stab Health} \times 0.10)}{10}$$

The result is capped between $0.0$ and $10.0$ and is then used to assign a Health Status (e.g., "EXCELLENT", "CRITICAL") based on defined thresholds (e.g., $9.0$ and above is "EXCELLENT").

## 3. Metric Progress Bar Health

The small progress bars beneath each metric use a simpler localized version of the same deviation logic found in the `updateMetricBlock()` method.

Instead of using the global $DEV$ constants, this local calculation defines a maximum deviation based on the optimal range of the metric itself. This is why the progress bar for Chlorine was still $33\%$ when the value was $0.0$: it was calculating the deviation from the optimal mid-point ($2.0$) against a $150\%$ range deviation ($3.0$).

To correct the visual output, we added a special condition: if Chlorine, Alkalinity, or Stabilizer (metrics that should never be zero) are exactly $0.0$, the progress bar is explicitly set to $0\%$ to clearly indicate a critical deficit.