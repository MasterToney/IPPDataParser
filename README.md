# Usage

The programm takes three arguments:

1. `exportedKnxAddresses.xml` the first is the exportet knx group Addresses in xml format produced by IPPDataExchange.
You can find the plugin and Addd it to your ETS project.
2. `itemNames.txt` contains optional display names for your items, each line represents a single item
3. `groups.txt` contains optional groups and their assigned items 

# How it works

This programm is based on our KNX installation, where all group Addresses are named "`function item`".
An item in this context is a thing like a lamp that you want to controll, depending on its type it might have multiple group Addresses assoziated with it e.g. one switch Address `LI item1` and a status Address `LI RM item1`.

From these two group Addresses a single openhab switch can be constructed e.g. `Switch item1 "Demo item1" (gAll, sampleGroup) { knx="0/1/0+0/1/1"}`

The display name comes from itemNames.txt `item1 Demo item1`

The group would come from groups.txt

```
Group gALL "EVERYTHING"
Group sampleGroup "Sample group display name" (gAll) item1;Item2
```

It is possible to  build a hierachy of groups and have an item in multiple groups.

The enired list of semantic group Address identifiers is as follows:
* Simple switches need `LI` and `RM LI`
* Dimmable lights need `LI` and `RM LI` `DIM`, `WE` and `RM WE` note that LI has no feedback as otherwise obenhab would sometimes display 100% brightness insteAdd of the percentage display
* Rollershutters `LZ`, `RM WE HÖ`, `KZ`
* Rollershutters angle controlling (single stepping) `KZ`
