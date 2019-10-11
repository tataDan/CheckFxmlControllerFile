CheckFxmlControllerFile is a Java command line application the takes exactly two arguments. It is used as follows: Java -jar CheckFxmlControllerFile.jar path_to_fxml_file path_to_controller_file.  It parses the fxml file and searches for elements that have "fx_id" and "onAction" attributes.  It then searches the corresponding Java controller file to determine whether or not it correctly reflects the relevant values of those elements.

For example, if the fxml file contains this code:

'<TextField fx:id="textField" onAction="#handleTextFieldAction" prefWidth="168.0" />',

then the application will search the controller file for something similar to this:

'@FXML
 private TextField textField;' and this:

'@FXML
 private void handleTextFieldAction() ...'

It outputs to the console the number of missing items and a list of the missing items.
For example:

0 MISSING ITEMS: 

or

2 MISSING ITEMS: 

handleButtonAction
TextField textField

