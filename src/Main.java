import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class Main {

    enum ITEM_TYPE {SWITCH, DIMMER, ROLLERSHUTTER, ROLLERSHUTTERSHORT, GROUP, UNKNOWN}



    Map<String, String> correlation;

    public static void main(String[] args) {
        ValidateArgs(args);

        System.out.print("Trying to open \"" + args[0] + "\" ... ");
        Main main = new Main();

        try {
            File inputFile = new File(args[0]);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            System.out.print("SUCCESS");
            doc.getDocumentElement().normalize();

            Scanner markingFile = new Scanner(new File(args[1]));

            main.correlation = new TreeMap<>();

            while (markingFile.hasNext()) {
                String s = markingFile.nextLine();
                String key = s.substring(0, s.indexOf(" "));
                String description = s.substring(s.indexOf(" ") + 1, s.length());
                // System.out.println(key + " " + description);
                main.correlation.put(key, description);
            }

            Node n = doc.getElementsByTagName("GrpAddrList").item(0);
            NodeList nl = n.getChildNodes();

            Map<String, Item> itemMap = new TreeMap<>();
            Map<String, List<String>> groupMap = new TreeMap<>();

            main.rec("0/", nl.item(1), itemMap);
            main.rec("1/", nl.item(3), itemMap);

            System.out.println(" ... import: SUCCESS!");

            String openHabianGroups = loadGroupCorrelations(new File(args[2]), itemMap, groupMap);

            System.out.println("Possible commands are: ");
            System.out.println("\"export\" [TYPE [A|AU|EG|K|OG]] were TYPE can be one of SWITCH, DIMMER, ROLLERSHUTTER, ROLLERSHUTTERshort ");
            System.out.println("or the id of an item ");
            System.out.print("ready for processing: \n");

            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                String s = sc.nextLine();
                String[] sArgs = s.split(" ");
                if (sArgs[0].equals("export")) {

                    if (sArgs.length == 1) {
                        System.out.println(openHabianGroups);
                        for (String key : itemMap.keySet()) {
                            if (key.matches("(A|AU|EG|K|OG|)\\d+") && itemMap.get(key).getType() == ITEM_TYPE.SWITCH) {
                                System.out.println(itemMap.get(key));
                            }
                        }
                        for (String key : itemMap.keySet()) {
                            if (key.matches("(A|AU|EG|K|OG|)\\d+") && itemMap.get(key).getType() == ITEM_TYPE.DIMMER) {
                                System.out.println(itemMap.get(key));
                            }
                        }
                        for (String key : itemMap.keySet()) {
                            if (key.matches("(A|AU|EG|K|OG|)\\d+") && itemMap.get(key).getType() == ITEM_TYPE.ROLLERSHUTTER) {
                                System.out.println(itemMap.get(key));
                            }
                        }
                        for (String key : itemMap.keySet()) {
                            if (key.matches("(A|AU|EG|K|OG|)\\d+short") && itemMap.get(key).getType() == ITEM_TYPE.ROLLERSHUTTERSHORT) {
                                System.out.println(itemMap.get(key));
                            }
                        }
                    } else {
                        String regEx = "";
                        if (sArgs.length == 3) {
                            regEx = "(" + sArgs[2] + "|)\\d+";
                        } else {
                            regEx = "(A|AU|EG|K|OG|)\\d+";
                        }
                        switch (sArgs[1]) {
                            case "SWITCH" :
                                for (String key : itemMap.keySet()) {
                                    if (key.matches(regEx) && itemMap.get(key).getType() == ITEM_TYPE.SWITCH) {
                                        System.out.println(itemMap.get(key));
                                    }
                                }
                                break;
                            case "DIMMER" :
                                for (String key : itemMap.keySet()) {
                                    if (key.matches(regEx) && itemMap.get(key).getType() == ITEM_TYPE.DIMMER) {
                                        System.out.println(itemMap.get(key));
                                    }
                                }
                                break;
                            case "ROLLERSHUTTER" :
                                for (String key : itemMap.keySet()) {
                                    if (key.matches(regEx) && itemMap.get(key).getType() == ITEM_TYPE.ROLLERSHUTTER) {
                                        System.out.println(itemMap.get(key));
                                    }
                                }
                                break;
                            case "ROLLERSHUTTERSHORT" :
                                for (String key : itemMap.keySet()) {
                                    if (key.matches(regEx + "short") && itemMap.get(key).getType() == ITEM_TYPE.ROLLERSHUTTERSHORT) {
                                        System.out.println(itemMap.get(key));
                                    }
                                }
                                break;
                                default:

                        }
                    }
                } else if (s.equals("printListOfItemTypes")) {
                    printListOfItemTypes(itemMap, ITEM_TYPE.SWITCH);
                    printListOfItemTypes(itemMap, ITEM_TYPE.DIMMER);
                    printListOfItemTypes(itemMap, ITEM_TYPE.ROLLERSHUTTER);
                    printListOfItemTypes(itemMap, ITEM_TYPE.ROLLERSHUTTERSHORT);
                } else if (itemMap.containsKey(s)) {
                    System.out.println(s + " found :)");

                    System.out.println(itemMap.get(s));

                } else if (groupMap.containsKey(s)) {
                    System.out.println(s + " found :)");
                    for (String halp: groupMap.get(s)) {
                        System.out.println(halp);
                    }
                } else {
                    System.out.println(s + " not found :(");
                }
            }

            sc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rec(String address, Node n, Map<String, Item> itemMap) {

        if (n.hasChildNodes()) {

            NodeList nList = n.getChildNodes();

            for (int i = 0; i < nList.getLength(); i++) {
                if (nList.item(i).getNodeName().equals("#text")) {
                    continue;
                }

                NamedNodeMap nnm = nList.item(i).getAttributes();

                if (nList.item(i).hasChildNodes()) {
                    rec(address + nnm.getNamedItem("Addr").getNodeValue() + "/", nList.item(i), itemMap);
                } else {

                    String id;
                    String type;
                    String name = nnm.getNamedItem("Name").getNodeValue().trim();
                    String adr = nnm.getNamedItem("Addr").getNodeValue();
                    if (!name.contains(" ")) {
                        id = name;
                        type = name;
                    } else {

                        if (!name.split(" ")[1].matches("\\d+")) {
                            id = name.substring(name.lastIndexOf(' ') + 1);
                            type = name.substring(0, name.lastIndexOf(' '));
                        } else {
                            id = name;
                            type = name;
                        }
                    }

                    if (itemMap.containsKey(id)) {
                        itemMap.get(id).groupAddresses.put(type, address + adr);
                        // KZ is for Kurz Zeit and means we have a rollershutter, create a new item with "short" added to the id
                        if (type.equals("KZ")) {
                            itemMap.put(id + "short", new Item(id + "short", type, address + adr));
                        }
                    } else {
                        itemMap.put(id, new Item(id, type, address + adr));
                    }
                }
            }
        }


    }

    class Item {
        String id;
        String groups;

        // first string is the key EG RM LI second String is group address
        Map<String, String> groupAddresses;

        public Item(String id, String addrType, String addr) {
            this.id = id;
            groups = "";
            groupAddresses = new HashMap<>();
            groupAddresses.put(addrType, addr);
        }

        public ITEM_TYPE getType() {
            switch (groupAddresses.size()) {
                case 0:
                    return ITEM_TYPE.GROUP;
                case 1:
                    return ITEM_TYPE.ROLLERSHUTTERSHORT;
                case 2:
                    return ITEM_TYPE.SWITCH;
                case 5:
                    return ITEM_TYPE.DIMMER;
                case 12:
                    return ITEM_TYPE.ROLLERSHUTTER;
                default:
                    return ITEM_TYPE.UNKNOWN;
            }
        }

        @Override
        public String toString() {
            String s = "";
            switch (groupAddresses.size()) {
                case 1:
                    s = String.format("Rollershutter %s \"%s\" (gRollerAll%s) { knx=\"%s\" }", id, id, groups, groupAddresses.get("KZ"));
                    break;
                case 2:
                    s = String.format("Switch %s \"%s %s\" (gSwitchAll%s) { knx=\"%s+<%s\"}", id, id, correlation.get(id), groups, groupAddresses.get("LI"), groupAddresses.get("RM LI"));
                    break;
                case 5:
                    // dimmer with status for ON OFF
                    // s = String.format("Dimmer %s \"%s %s [%%s]\" (gDimmerAll%s) { knx=\"%s+%s, %s, %s+%s\" }", id, id, correlation.get(id), groups, groupAddresses.get("LI"), groupAddresses.get("RM LI"), groupAddresses.get("DIM"), groupAddresses.get("WE"), groupAddresses.get("RM WE"));

                    // dimmer WITHOUT ON OFF FEEDBACK
                    s = String.format("Dimmer %s \"%s %s [%%s]\" (gDimmerAll%s) { knx=\"%s, %s, %s+<%s\" }", id, id, correlation.get(id), groups, groupAddresses.get("LI"), groupAddresses.get("DIM"), groupAddresses.get("WE"), groupAddresses.get("RM WE"));
                    break;
                case 12:
                    s = String.format("Rollershutter %s \"%s %s\" (gRollerAll%s) { knx=\"1.008:%s+<%s, %s\" }", id, id, correlation.get(id), groups, groupAddresses.get("LZ"), groupAddresses.get("RM WE HÖ"), groupAddresses.get("KZ"));
                    break;
                default:
                    s = "unknown type :/ " + id;
                    for (String key : groupAddresses.keySet()) {
                        s += ("\n" + groupAddresses.get(key) + " " + key);
                    }
            }

            return s;
        }
    }

    public static String loadGroupCorrelations(File file, Map<String, Item> itemMap, Map<String, List<String>> groupMap) throws FileNotFoundException {
        System.out.print("Trying to open \"" + file.getAbsolutePath() + "\" ... ");
        Scanner groupScanner = new Scanner(file);
        System.out.println("SUCCESS");
        int countOfGroupAssignments = 0;
        String openHabianGroups = "";

        while (groupScanner.hasNext()) {
            String s = groupScanner.nextLine();
            String[] sArr = s.split(" ");

            // Skip comments and blank lines
            if (sArr.length < 3 || s.startsWith("//") || s.isEmpty() || sArr[sArr.length - 1].endsWith("\"")) {
                openHabianGroups += s + "\n";
                continue;
            }



            String groupName = sArr[1];
            String parentGroupName = "";


            if (!groupMap.containsKey(groupName)) {
                groupMap.put(groupName, new LinkedList<>());
            }

            // lonely groups only
            if (sArr[sArr.length - 1].endsWith(")") ) {
                // like: Group gEG (ALL)
                openHabianGroups += s + "\n";
                parentGroupName = sArr[sArr.length - 1].substring(1, sArr[sArr.length - 1].length() - 1);
            } else {
                //sArr[sArr.length - 2].substring(1, sArr[sArr.length - 1].length() - 1);
                openHabianGroups += s.substring(0, s.indexOf(sArr[sArr.length - 1]) ) + "\n";
                for (String key : sArr[sArr.length - 1].split(";")) {
                    if (itemMap.containsKey(key)) {
                        groupMap.get(groupName).add(key);
                        itemMap.get(key).groups += ", " + groupName;
                        countOfGroupAssignments++;
                    } else {
                        System.out.println(key + " wasn't found");
                    }
                }
            }
            if(!groupMap.containsKey(parentGroupName)) {
                groupMap.put(parentGroupName, new LinkedList<>());
            }
            groupMap.get(parentGroupName).add(groupName);
        }

        System.out.println("Assigned " + countOfGroupAssignments + " groups to items.");
        return openHabianGroups;
    }


    public static void printListOfItemTypes(Map<String, Item> itemMap, ITEM_TYPE item_type) {
        for (Item item : itemMap.values() ) {
            if (item.getType() == item_type) {
                System.out.print(item.id + " ");
            }
        }
        System.out.println();
    }

    private static void ValidateArgs(String[] args) {
        if (args.length != 3) {
            System.out.println("USAGE: Main groupAddresses.xml designation.txt groups.txt\nFor more information about the content of these files, please refer to the README");
            System.exit(0);
        }
    }
}
