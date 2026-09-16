import {ConditionItem} from "./condition-item";
import {SymptomFactor} from "./symptom-factor";

/**
 * A ranked possible cause. `score` is the share of this cause's own description that the reader
 * ticked, not a probability - `matchedFactorCodes` is what the UI shows to explain the ordering.
 */
export interface PossibleCause {
    causeText: string;
    factorsText: string;
    conditions: ConditionItem[];
    factors: SymptomFactor[];
    matchedFactorCodes: string[];
    score: number;
    matchedCount: number;
}
