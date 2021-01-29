package consoleUtilities;

public abstract class Display {
	
	private boolean isFocused;
	
	/**
	 * Print a header to a column. This will center the title in the line of *s
	 * @param data Title of column
	 * @param colTabWidth Width of column in tabs
	 */
	public void printHead(String data, int colTabWidth) {
		int textIndex = (colTabWidth * 4) - (data.length() / 2);
		for(int i = 0 ; i < colTabWidth * 8 + 2; i++) {
			if(i == textIndex) {
				System.out.print(data);
				i += data.length();
			}else {
				System.out.print("*");
			}
		}
	}
	
	/**
	 * Print a footer to close a column.
	 * @param colTabWidth Width of column in tabs
	 */
	public void printEnd(int colTabWidth) {
		for(int i = 0; i < colTabWidth; i++) {
			System.out.print("********");
		}
		System.out.print("*");
	}
	
	/**
	 * Prints a line of text in a column. It will auto format the line to contain only the max chars a column can have. 
	 * @param data The text to be printed in the column. 
	 * @param colTabWidth Width of column in tabs
	 */
	public void printCol(String data, int colTabWidth) {
		if(data.length() > (colTabWidth * 8.0) - 1) {
			System.out.print("*" + data.substring(0, (colTabWidth * 8) - 4) + "...");
		}else {
			System.out.print("*" + data);
		}
		for(int j = 0; j < Math.ceil(((colTabWidth * 8.0) - data.length() - 1) / 8.0); j++) {
			System.out.print("\t");
		}
		System.out.print("*");
		
	}
	
	abstract void drawDisplay();

	public boolean isFocused() {
		return isFocused;
	}

	public void setFocused(boolean isFocused) {
		this.isFocused = isFocused;
		if(isFocused) {
			drawDisplay();
		}
	}

}
