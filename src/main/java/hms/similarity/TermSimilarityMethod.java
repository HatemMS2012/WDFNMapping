package hms.similarity;

public enum TermSimilarityMethod {
	GreedyStringTiling, BoundedSubstringMatch,CosineSimilarity,JaroSecondString,JaroWinklerSecondString,
	Levenshtein,LevenshteinSecondString,LongestCommonSubsequence,LongestCommonSubsequenceNorm,
	LongestCommonSubstring,MongeElkanSecondString,SubstringMatch, WordNet;
}
