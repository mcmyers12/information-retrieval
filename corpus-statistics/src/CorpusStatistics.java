import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CorpusStatistics {

	private static final String BIBLE_FILE = "bible-asv.txt";
	private static final String LES_MIS_FILE = "lesmis.txt";

	private int numDocuments = 0; // Number of paragraphs processed
	private int vocabularySize = 0; // Number of unique words observed
	private int collectionSize = 0; // Total number of words encountered
	private int numWordsInOneDocument = 0; // Number of words that occur in
											// exactly one document
	private Map<String, Term> lexicon = new HashMap<>();

	private class Term {
		private int collectionFrequency = 0; // Number of times term is seen
		private int documentFrequency = 0; // Number of documents which the word
											// occurs in
	}

	private void readFile(String fileName) {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(fileName);
			bufferedReader = new BufferedReader(fileReader);

			String currentLine;

			while ((currentLine = bufferedReader.readLine()) != null) {
				if (currentLine.startsWith("<P ID=")) {
					currentLine = bufferedReader.readLine();
					String[] tokens = tokenize(currentLine);
				}
				System.out.println(currentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Split on spaces, Lower case only if not all upper case, Remove leading
	 * and trailing punctuation from each token
	 * 
	 * @param line
	 * @return
	 */
	private String[] tokenize(String line) {
		String[] tokens = line.split("\\s");
		for (int i = 0; i < tokens.length; i++) {
			if (!tokens[i].toUpperCase().equals(tokens[i])) {
				tokens[i] = tokens[i].toLowerCase();
			}
			tokens[i] = tokens[i].replaceFirst("^[^a-zA-Z]+", "");
			tokens[i] = tokens[i].replaceAll("[^a-zA-Z]+$", "");
		}

		return tokens;
	}

	// Calculate document frequency: number of documents a term appears in
	// Calculate collection frequency: number of times the word appears
	// Test program on two text files
	public static void main(String[] args) {
		CorpusStatistics corpusStatistics = new CorpusStatistics();
		// corpusStatistics.readFile(CorpusStatistics.BIBLE_FILE);
		for (String token : corpusStatistics.tokenize("''H-i Mir'anda... HEY")) {
			System.out.println(token);
		}

	}

}
