import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
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
public class InvertedFileBuilder {
	private String fileName; // Name of input file for which to create inverted file
	private Map<String, Term> lexicon = new HashMap<>();  // Lexicon that will hold all terms
	private Map<String, List<InvertedFileRecord>> invertedFileRecords = new TreeMap<>();	// Map containing each inverted file record sorted by term, then docId
	private File invertedBinaryFile = new File("inverted-file.bin");
	private File dictionaryFile = new File("dictionary.ser");
	private final int INT_SIZE = 4;  // Number of bytes per int

	public InvertedFileBuilder(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Class representing a term that is used to build the lexicon and list of terms
	 */
	private static class Term implements Serializable {
		private static final long serialVersionUID = -1947264671039701464L;
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
		private int documentId;
		private int count;

		public InvertedFileRecord(int documentId, int count) {
			this.documentId = documentId;
			this.count = count;
		}

		@Override
		public String toString() {
			return "(documentID: " + documentId
					+ ", count: " + count + ")";
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

		while ((currentLine = bufferedReader.readLine()) != null) {
			if (currentLine.startsWith("<P ID=")) { // The start of a new document (paragraph)
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
					invertedFileRecords.get(token).add(new InvertedFileRecord(documentId, count));
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
	public void createInvertedIndex() {
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

	public void writeDictionaryToFile() throws IOException {
		FileOutputStream fos = new FileOutputStream(dictionaryFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(lexicon);
		oos.close();
	}

	@SuppressWarnings("unchecked")
	public void readDictionaryFromFile() throws IOException, ClassNotFoundException {
		InputStream file = new FileInputStream(dictionaryFile);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream (buffer);
		lexicon = (Map<String, Term>) input.readObject();
		input.close();
	}

	/**
	 * Read the binary file after creating the index
	 * Print out the document frequency and posting list for terms: study, plutonium, Rome, Athens, feasts
	 * Print out the document frequency for words horse, lovingkindness, Mary, dance
	 * @throws IOException
	 */
	public List<InvertedFileRecord> readInvertedIndex(String token) throws IOException {
		List<InvertedFileRecord> invertedFileRecordList = new ArrayList<>();
		RandomAccessFile randomAccessFile = new RandomAccessFile(invertedBinaryFile, "r");

		Term term = lexicon.get(token);

		if (term != null) {
			int documentFrequency = term.documentFrequency * 2;  // The number of integers stored for the token

			int filePointer = term.invertedFileLocation;

			for (int i = 0; i < documentFrequency; i+=2) {
				randomAccessFile.seek(filePointer);
				int documentId = randomAccessFile.readInt();
				filePointer += INT_SIZE;

				randomAccessFile.seek(filePointer);
				int count = randomAccessFile.readInt();
				filePointer += INT_SIZE;

				invertedFileRecordList.add(new InvertedFileRecord(documentId, count));
			}
		}
		randomAccessFile.close();
		return invertedFileRecordList;
	}

	public void prettyPrintList(List<InvertedFileRecord> invertedFileRecordList) {
		if (invertedFileRecordList.isEmpty()) {
			System.out.println("\tnone");
		}
		else {
			for (InvertedFileRecord invertedFileRecord : invertedFileRecordList) {
				System.out.println("\t" + invertedFileRecord);
			}
		}
	}

	public void testInvertedFile() throws IOException {
		System.out.println("\n\n\nDocument frequencies and postings lists:");
		System.out.println("  study");
		prettyPrintList(readInvertedIndex("study"));
		System.out.println("  plutonium");
		prettyPrintList(readInvertedIndex("plutonium"));
		System.out.println("  rome");
		prettyPrintList(readInvertedIndex("rome"));
		System.out.println("  athens");
		prettyPrintList(readInvertedIndex("athens"));
		System.out.println("  feasts");
		prettyPrintList(readInvertedIndex("feasts"));

		System.out.println("\nDocument frequencies:");
		System.out.println("  horse\n\t" + lexicon.get("horse").documentFrequency);
		System.out.println("  lovingkindness\n\t" + lexicon.get("lovingkindness").documentFrequency);
		System.out.println("  mary\n\t" + lexicon.get("mary").documentFrequency);
		System.out.println("  dance\n\t" + lexicon.get("dance").documentFrequency);
	}

	public void testInvertedFileBuilder() throws ClassNotFoundException, IOException {
		//Build the dictionary
		buildLexicon();

		//Create the inverted file and write to binary file
		createInvertedIndex();

		//Write the lexicon to disk
		writeDictionaryToFile();

		//Read the lexicon from disk
		readDictionaryFromFile();

		//Read test terms, postings lists, and document frequencies from the inverted file
		testInvertedFile();
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		InvertedFileBuilder invertedFileBuilder = new InvertedFileBuilder("bible-asv.txt");
		invertedFileBuilder.testInvertedFileBuilder();;
	}
}



