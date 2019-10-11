package checkfxmlcontrollerfile;

/*
This program parses a fxml file and searches for elements that have fx:id and 
onAction attributes. It then searches the corresponding Java controller file to
determine whether or not it correctly reflects the values of those elements.
For example, if the fxml file contains this code:
"<TextField fx:id="textField" onAction="#handleTextFieldAction" prefWidth="168.0" />",
then the controller file should contain something similar to this:
"@FXML private TextField textField;" and this:
"	@FXML
	private void handleTextFieldAction() {
		outputTextArea.appendText("TextField Action\n");
	}".  
*/

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;
import java.util.regex.*;

public class CheckFxmlControllerFile {

	static List<String> requiredItems = new ArrayList<String>();
	static List<String> missingItems = new ArrayList<String>();

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

		// Check how many arguments were passed in
		if (args.length != 2) {
			System.out.println("Usage: java program fxml_filename controller_filename");
			System.exit(1);
		}

		String fxmlFileName = args[0];
		String controllerFileName = args[1];
		
		// Read the controller file into a String and then remove all comments from that String
		String controllerContents = readFileIntoString(controllerFileName);
		controllerContents = controllerContents.replaceAll("([\\t ]*\\/\\*(?:.|\\R)*?\\*\\/[\\t ]*\\R?)|(\\/\\/.*)",
				"");

		// Parse the FXML file
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder loader = factory.newDocumentBuilder();
		Document document = loader.parse(fxmlFileName);
		DocumentTraversal traversal = (DocumentTraversal) document;

		TreeWalker walker = traversal.createTreeWalker(document.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, null, true);

		traverseLevel(walker, "");

		searchForRequiredItems(controllerContents);

		int numberOfMissingItems = missingItems.size();
		
		System.out.println("\n" + numberOfMissingItems + " MISSING ITEMS: \n");
		for (String item : missingItems) {
			System.out.println(item);
		}
		System.out.println();
		
		// Return "2" to indicate that the program ran successfully but found missing
		// items in case the user wants to use the return value in a batch file
		if (numberOfMissingItems > 0) {
			System.exit(2);
		}
	}

	private static void traverseLevel(TreeWalker walker, String indent) {

		Node node = walker.getCurrentNode();

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			// Check all attributes
			if (node.hasAttributes()) {
				// get attributes names and values
				NamedNodeMap nodeMap = node.getAttributes();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					Node tempNode = nodeMap.item(i);
					if (nodeMap.item(i).toString().contains("fx:id")) {
						requiredItems.add(node.getNodeName() + " " + tempNode.getNodeValue());
					}
					if (nodeMap.item(i).toString().contains("onAction")) {
						requiredItems.add(tempNode.getNodeValue().replace("#", ""));
					}
				}
			}
		}

		for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {

			traverseLevel(walker, indent + "  ");
		}

		walker.setCurrentNode(node);
	}

	private static String readFileIntoString(String filePath) throws IOException {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return contentBuilder.toString();
	}

	private static void searchForRequiredItems(String controllerContents) {

		for (String item : requiredItems) {
			String pattern;
			
			// Check for fields
			if (item.contains(" ")) {
				String[] parts = item.split(" ");

				// Check for field with either a private, protected or
				// no access modifier
				// It must have @FXML annotation
				pattern = "(?s).*" + "@FXML" + "\\s+" + "(private |protected )?" + parts[0] + "\\s+" + parts[1] + "\\s*;" + ".*";
				if (Pattern.matches(pattern, controllerContents)) {
					continue;
				}
				
				// Check for field with public access modifier
				// The @FXML annotation is not required but may be present
				pattern = "(?s).*" + "public" + "\\s+" + parts[0] + "\\s+" + parts[1] + "\\s*;" + ".*";
				if (Pattern.matches(pattern, controllerContents)) {
					continue;
				}

				// Check for generic field with either a private, protected, or
				// no access modifier
				// It must have @FXML annotation
				pattern = "(?s).*" + "@FXML" + "\\s+" + "(private |protected )?" + parts[0] + "\\s*<" + "\\s*" + ".+"
						+ "\\s*>" + "\\s+" + parts[1] + "\\s*;" + ".*";
				if (Pattern.matches(pattern, controllerContents)) {
					continue;
				}

				// Check for generic field with public access modifier
				// The @FXML annotation is not required but may be present
				pattern = "(?s).*" + "\\s*" + "public" + "\\s+" + parts[0] + "\\s*<" + "\\s*" + ".+" + "\\s*>" + "\\s+"
						+ parts[1] + "\\s*;" + ".*";
				if (Pattern.matches(pattern, controllerContents)) {
					continue;
				}
			}
		
			// Check for onAction event handler with either a private, protected or
			// no access modifier
			// It must have @FXML annotation
			pattern = "(?s).*" + "@FXML" + "\\s+" + "(private |protected )?" + "void" + "\\s+" + item + "\\s*\\(" + "\\s*\\)" + ".*";
			if (Pattern.matches(pattern, controllerContents)) {
				continue;
			}
		
			// Check for public onAction event handler
			// The @FXML annotation is not required but may be present
			pattern = "(?s).*" + "\\s*" + "public" + "\\s+" + "void" + "\\s+" + item + "\\s*\\(" + "\\s*\\)"  + ".*";
			if (Pattern.matches(pattern, controllerContents)) {
				continue;
			}

			missingItems.add(item);

		}
	}
}
