/**
 * This class builds an inverted file that contains a postings list for each dictionary term
 * It also writes the dictionary to disk
 * For Each word in the lexicon, a file offset to the corresponding on-disk posting list is stored
 *
 * This program follows the memory-based inversion algorithm (Algorithm A)
 * 		It writes out the postings after all documents have been read
 * @author mirandamyers
 *
 */
public class InvertedFile {

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

	/**
	 * Write to a binary file using 4-byte integers for document ids
	 * and 4-byte integers for document term frequency
	 * Also suggested to store the length of the postings list (ie. document frequency) with other info in dictionary data structure
	 */
	public void writeToBinaryFile() {

	}

	/**
	 * Read the binary file after creating the index
	 * Print out the document frequency and posting list for terms: study, plutonium, Rome, Athens, feasts
	 * Print out the document frequency for words horse, lovingkindness, Mary, dance
	 */
	public void readBinaryFileTest() {

	}
}
