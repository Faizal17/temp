package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "cards")
class CreditCardListWrapper {

    private List<CreditCard> cards;

    @XmlElement(name = "card")
    public List<CreditCard> getCards() {
        return cards;
    }

    public void setCards(List<CreditCard> cards) {
        this.cards = cards;
    }
}

// Base CreditCard class and its subclasses
abstract class CreditCard {
    protected String cardNumber;
    protected String expirationDate;
    protected String cardHolder;

    public CreditCard(String cardNumber, String expirationDate, String cardHolder) {
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cardHolder = cardHolder;
    }

    // Additional methods like getters, setters, toString, etc.
}

class VisaCC extends CreditCard {
    public VisaCC(String cardNumber, String expirationDate, String cardHolder) {
        super(cardNumber, expirationDate, cardHolder);
    }
}

class MasterCC extends CreditCard {
    public MasterCC(String cardNumber, String expirationDate, String cardHolder) {
        super(cardNumber, expirationDate, cardHolder);
    }
}

class AmExCC extends CreditCard {
    public AmExCC(String cardNumber, String expirationDate, String cardHolder) {
        super(cardNumber, expirationDate, cardHolder);
    }
}

// CreditCardFactory with validation methods
class CreditCardFactory {
    public static CreditCard createCreditCard(String cardNumber, String expirationDate, String cardHolder) {
        if (isValidVisa(cardNumber)) {
            return new VisaCC(cardNumber, expirationDate, cardHolder);
        } else if (isValidMaster(cardNumber)) {
            return new MasterCC(cardNumber, expirationDate, cardHolder);
        } else if (isValidAmEx(cardNumber)) {
            return new AmExCC(cardNumber, expirationDate, cardHolder);
        }
        return new AmExCC(cardNumber, expirationDate, cardHolder);
        // throw new IllegalArgumentException("Invalid credit card number " +
        // cardNumber);
    }

    private static boolean isValidVisa(String cardNumber) {
        return cardNumber.startsWith("4") && (cardNumber.length() == 13 || cardNumber.length() == 16);
    }

    private static boolean isValidMaster(String cardNumber) {
        return cardNumber.startsWith("5") && (cardNumber.length() == 16);
    }

    private static boolean isValidAmEx(String cardNumber) {
        return cardNumber.startsWith("3") && (cardNumber.length() == 15);
    }
}

// FileParser interface (Strategy interface)
interface FileParser {
    List<CreditCard> parseFile(String filePath) throws IOException, JAXBException;

    void writeFile(List<CreditCard> cards) throws IOException, JAXBException;
}

// Concrete strategies for CSV, JSON, XML parsing
class CSVParser implements FileParser {
    @Override
    public List<CreditCard> parseFile(String filePath) throws IOException {
        List<CreditCard> cards = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            try {
                while ((line = reader.readNext()) != null) {
                    CreditCard card = CreditCardFactory.createCreditCard(line[0], line[1], line[2]);
                    cards.add(card);
                }
            } catch (CsvValidationException e) {
                e.printStackTrace();
            }
        }
        return cards;
    }

    @Override
    public void writeFile(List<CreditCard> cards) throws IOException {
        String filePath = "output_file.csv"; // example file path
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            for (CreditCard card : cards) {
                String[] line = { card.cardNumber, card.getClass().getSimpleName() };
                writer.writeNext(line);
            }
        }
    }
}

class JSONParser implements FileParser {
    @Override
    public List<CreditCard> parseFile(String filePath) throws IOException {
        Gson gson = new Gson();
        try (JsonReader reader = new JsonReader(new FileReader(filePath))) {
            Type listType = new TypeToken<ArrayList<CreditCard>>() {
            }.getType();
            return gson.fromJson(reader, listType);
        }
    }

    @Override
    public void writeFile(List<CreditCard> cards) throws IOException {
        String filePath = "output_file.json"; // example file path
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(cards, writer);
        }
    }
}

class XMLParser implements FileParser {
    @Override
    public List<CreditCard> parseFile(String filePath) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(CreditCardListWrapper.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        CreditCardListWrapper wrapper = (CreditCardListWrapper) unmarshaller.unmarshal(new File(filePath));
        return wrapper.getCards();
    }

    @Override
    public void writeFile(List<CreditCard> cards) throws JAXBException, IOException {
        String filePath = "output_file.xml"; // example file path
        JAXBContext context = JAXBContext.newInstance(CreditCardListWrapper.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        CreditCardListWrapper wrapper = new CreditCardListWrapper();
        wrapper.setCards(cards);
        try (FileWriter writer = new FileWriter(filePath)) {
            marshaller.marshal(wrapper, writer);
        }
    }
}

// Context class
class CreditCardFileProcessor {
    private FileParser parser;

    public void setParser(FileParser parser) {
        this.parser = parser;
    }

    public void processFile(String inputFilePath) throws IOException, JAXBException {
        List<CreditCard> cards = parser.parseFile(inputFilePath);
        parser.writeFile(cards);
    }
}

// Main Application
public class Main {
    public static void main(String[] args) {
        CreditCardFileProcessor processor = new CreditCardFileProcessor();

        String inputFilePath = "./input_file.csv";
        if (inputFilePath.endsWith(".csv")) {
            processor.setParser(new CSVParser());
        } else if (inputFilePath.endsWith(".json")) {
            processor.setParser(new JSONParser());
        } else if (inputFilePath.endsWith(".xml")) {
            processor.setParser(new XMLParser());
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }

        try {
            processor.processFile(inputFilePath);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            // Handle exceptions
        }
    }
}

// Note: Implementations for CSVParser, JSONParser, and XMLParser classes are
// placeholders.
// You would need to fill in the details for parsing and writing files using
// respective libraries.