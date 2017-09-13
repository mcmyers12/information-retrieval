import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class builds an inverted file that contains a postings list for each dictionary term
 * It also writes the dictionary to disk
 * For Each word in the lexicon, a file offset to the corresponding on-disk posting list is stored
 *
 * This program follows the memory-based inversion algorithm (Algorithm A)
 * 		It writes out the postings after all documents have been read
 * @author Miranda Myers
 *
 */
public class InvertedFile {
	private String fileName; // Name of input file for which to create inverted file
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms
	private Map<String, List<InvertedFileRecord>> invertedFileRecords = new TreeMap<>();	// Map containing each inverted file record sorted by term, then docId
	private File invertedBinaryFile = new File("inverted-file.bin");
	private File dictionaryFile = new File("dictionary.ser");
	private final int INT_SIZE = 4;

	public InvertedFile(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Class representing a term that is used to build the lexicon and list of terms
	 */
	private class Term {
		private String text;	//Required in memory
		private int invertedFileLocation;	//Required in memory

		private int collectionFrequency = 0; // Number of times term is seen
		private int documentFrequency = 0; // Number of documents which the word occurs in	// Useful in memory

		@Override
		public String toString() {
			return text + "\t\t - collection frequency: " + collectionFrequency
					+ ", document frequency: " + documentFrequency;
		}
	}


	private class InvertedFileRecord {
		private String term;
		private int documentId;
		private int count;

		public InvertedFileRecord(String term, int documentId, int count) {
			this.term = term;
			this.documentId = documentId;
			this.count = count;
		}

		@Override
		public String toString() {
			return term + "  - documentID: " + documentId
					+ ", count: " + count;
		}
	}

	/**
	 * Split on spaces
	 * Lower case only if not all upper case
	 * Remove leading and trailing punctuation from each token
	 *
	 * @param line
	 * @return list of tokens
	 */
	private List<String> tokenize(String line) {
		String[] tokens = line.split("\\s+"); // Split string on whitespace
		List<String> normalizedTokens = new ArrayList<>();
		for (String token : tokens) {
			String normalizedToken = token.toLowerCase();

			//Remove external punctuation
			normalizedToken = normalizedToken.replaceFirst("^[^a-zA-Z]+", "");
			normalizedToken = normalizedToken.replaceAll("[^a-zA-Z]+$", "");

			if (!normalizedToken.equals("")) {
				normalizedTokens.add(normalizedToken);
			}
		}

		return normalizedTokens;
	}

	/**
	 * Read file and build lexicon
	 */
	public void buildLexicon() {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(fileName);
			bufferedReader = new BufferedReader(fileReader);

			buildLexicon(bufferedReader);

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
	 * Build lexicon helper
	 * @param bufferedReader
	 * @throws IOException
	 */
	private void buildLexicon(BufferedReader bufferedReader) throws IOException {
		String currentLine;

		int ct = 0;
		while ((currentLine = bufferedReader.readLine()) != null && ct < 3) {
			if (currentLine.startsWith("<P ID=")) { // The start of a new document (paragraph)

				ct++;

				int documentId = Integer.parseInt(currentLine.replace("<P ID=", "").replace(">", ""));

				currentLine = bufferedReader.readLine();
				Map<String, Integer> tokensInDocument = new HashMap<>();

				while (currentLine != null && !currentLine.startsWith("</P>")) {
					List<String> tokens = tokenize(currentLine);

					for (String token : tokens) {
						if (tokensInDocument.containsKey(token)) {
							int count = tokensInDocument.get(token).intValue();
							tokensInDocument.put(token, ++count);
						}
						else {
							tokensInDocument.put(token, 1);
						}

						if (lexicon.containsKey(token)) {
							lexicon.get(token).collectionFrequency++; // Increment number of times term is seen
						}
						else {
							Term term = new Term();
							term.text = token;
							term.collectionFrequency++;
							lexicon.put(token, term);
						}
					}

					currentLine = bufferedReader.readLine();
				}

				for (String token : tokensInDocument.keySet()) {
					lexicon.get(token).documentFrequency++; // Increment number of documents each token occurs in

					int count = tokensInDocument.get(token);
					//lexicon.get(token).invertedFileRecords.add(new InvertedFileRecord(documentId, count));

					if (!invertedFileRecords.containsKey(token)) {
						invertedFileRecords.put(token, new ArrayList<InvertedFileRecord>());
					}
					invertedFileRecords.get(token).add(new InvertedFileRecord(token, documentId, count));
				}
			}
		}
	}


	/**
	 * Write to a binary file using 4-byte integers for document ids
	 * and 4-byte integers for document term frequency
	 * Also suggested to store the length of the postings list (ie. document frequency) with other info in dictionary data structure
	 * @throws IOException
	 */
	public void writeToBinaryFile() {
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(invertedBinaryFile, "rw"); // Open inverted binary file for writing
			int filePointer = 0;
			for (Entry<String, List<InvertedFileRecord>> invertedFileRecordsEntry: invertedFileRecords.entrySet()) {
				String token = invertedFileRecordsEntry.getKey();
				List<InvertedFileRecord> invertedFileRecordList = invertedFileRecordsEntry.getValue();

				lexicon.get(token).invertedFileLocation = filePointer;

				for (InvertedFileRecord invertedFileRecord : invertedFileRecordList) {
					randomAccessFile.writeInt(invertedFileRecord.documentId);
					filePointer += INT_SIZE;

					randomAccessFile.writeInt(invertedFileRecord.count);
					filePointer += INT_SIZE;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}

	/**
	 * Read the binary file after creating the index
	 * Print out the document frequency and posting list for terms: study, plutonium, Rome, Athens, feasts
	 * Print out the document frequency for words horse, lovingkindness, Mary, dance
	 * @throws IOException
	 */
	public void readInvertedIndex(String token) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(invertedBinaryFile, "r");

		System.out.println(token);
		int documentFrequency = lexicon.get(token).documentFrequency * 2;  // The number of integers stored for the token

		int filePointer = lexicon.get(token).invertedFileLocation;

		for (int i = 0; i < documentFrequency; i++) {
			randomAccessFile.seek(filePointer);
			int readInt = randomAccessFile.readInt();
			System.out.println(readInt);

			filePointer += INT_SIZE;
		}


	}

	public static void main(String[] args) throws IOException {
		InvertedFile invertedFile = new InvertedFile("bible-asv.txt");
		invertedFile.buildLexicon();
		invertedFile.writeToBinaryFile();

		//		for (Map.Entry<String, List<InvertedFileRecord>> entry : invertedFile.invertedFileRecords.entrySet()) {
		//			System.out.println(entry);
		//		}
		for (List<InvertedFileRecord> ifr : invertedFile.invertedFileRecords.values()) {
			System.out.println(ifr);
		}


		invertedFile.readInvertedIndex("and");
	}
}

//Questions
//		Do we have to use term ids?


