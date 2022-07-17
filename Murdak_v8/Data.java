package Murdak_v8;

import aic2022.user.*;

public class Data {

    UnitController uc;
    Tools tools;

    //GENERAL INFO
    Direction[] dirs = Direction.values();
    int ID;                     UnitType type;
    Location allyBase; int baseCode = 100; //the first position where we store our map
    Team allyTeam;              Team enemyTeam;
    int currentRound;           int spawnRound;                 int turnsAlive;
    int x;                      int y;                          int z;
    Location enemyLoc;          boolean enemyFound = false;
    Location enemyBaseLoc;      boolean enemyBaseFound = false;
    boolean baseInDanger = false;

    //CHARACTER SPECIFIC INFO
    Direction prefDir;
    Boolean inDungeon;
    Boolean escapeDungeon;

    //CONSTANTS
    int accumulationRound = 250; //Round until we keep combat units around the base
    int dungeonExplorationRound = 10; //Round where we start entering dungeons
    int shrineDistanceThreshold = 1800; //Distance at which we start destroying shrines

    int reinforcementDist = 4000; //Max distance to walk to enemyLoc
    int reinforcementRound = 150; //Round at which we start sending reinforcements

    int seekChestDist = 64; //max distance at which scouts go open chests
    int seekShrineDist = 49; //max distance at which units go conquer shrines

    int rangerLvlThreshold = 40;    //minimum reputation we need to have to upgrade ranger
    int barbarianLvlThreshold = 60; //minimum reputation we need to have to upgrade barbarian
    int knightLvlThreshold = 60;    //minimum reputation we need to have to upgrade knight

    //UNITS CHANNELS
    int unitCh,             unitReportCh,           unitResetCh;            // Ch 0, 1, 2
    int scoutCh,            scoutReportCh,          scoutResetCh;           // Ch 3, 4, 5
    int barbarianCh,        barbarianReportCh,      barbarianResetCh;       // Ch 6, 7, 8
    int rangerCh,           rangerReportCh,         rangerResetCh;          // Ch 9, 10, 11
    int mageCh,             mageReportCh,           mageResetCh;            // Ch 12, 13, 14
    int knightCh,           knightReportCh,         knightResetCh;          // Ch 15, 16, 17
    int assassinCh,         assassinReportCh,       assassinResetCh;        // Ch 18, 19, 20
    int clericCh,           clericReportCh,         clericResetCh;          // Ch 21, 22, 23

    // Unit Count Info
    int nUnits,     nScouts,      nBarbarians,     nRangers;
    int nMages,     nKnights,     nAssassins,      nClerics;

    // Map info
    public class Tile{
        boolean negativecoords;
        TileType tileType;

    }
    // Shrines info
    int[] shrineCh = {24,44}; int lastShrine = shrineCh[0];


    //MAP CHANNELS - from 10000 to 18000
    //  the 1st digit we save is the code of that tile type "t", the 2nd is the direction
    //  towards our base ">", and the next three the distance to our base, "ddd".
    //  from digits 6 to 9 we save the same info regarding the enemy base.
    //  So, the format is "ddd>ddd>t"

    //DUNGEON MAP CHANNELS - from 20000 to 28000
    //  the n-th digits of ch "2xxyy" represents the direction towards the n-th dungeon.

    //BFS CHANNELS - from 100000 to _
    int baseBFSCh =         100000;
    int enemyBaseBFSCh =    110000;
    int[] dungeonCh = {50,51,52,53,54,55,56,57};      // We can target up to 8 dungeons.
    int currentDungeon = -1;
    int[] dungeonBFSCh = {120000, 130000, 140000, 150000, 160000, 170000, 180000, 190000};

    //GENERAL CHANNELS
    int accumulationCh = 100; // Ch 100
    int baseLocationCh = 101; // Ch 101
    int enemyFoundCh = 102;         int enemyLocCh = 103;   //Ch 102 & 103
    int enemyBaseFoundCh = 104;     int enemyBaseLocCh = 105;   //Ch 104 & 105
    int baseDangerCh = 106;



    public Data (UnitController _uc) {
        uc = _uc;
        tools = new Tools(uc, this);
        ID = uc.getInfo().getID();
        type = uc.getType();
        allyTeam = uc.getTeam();
        enemyTeam = uc.getOpponent();
        currentRound = uc.getRound();

        //Explorer initialization
        prefDir = tools.randomDir(); //TODO set to unexplored movable tile
        inDungeon = false;
        escapeDungeon = false;
    }

    // This function is called once per turn
    public void update() {

        // General updates
        updateGeneral();
        updateChannels();
        updateUnitCountInfo();

        // Resource updates
        //updateShrines();

        // Army updates
        //updateEnemyIntel();

        // Class specific updates
        //updateBase();
    }

    void updateGeneral() {
        currentRound = uc.getRound();
        turnsAlive = currentRound - spawnRound;
        allyBase = tools.decodeLoc(uc.readOnSharedArray(baseLocationCh));
        x = currentRound%3;
        y = (currentRound+1)%3;
        z = (currentRound+2)%3;

        enemyLoc   = tools.decodeLoc(uc.readOnSharedArray(enemyLocCh) );
        enemyFound = (uc.readOnSharedArray(enemyFoundCh) == 1);

        enemyBaseLoc   = tools.decodeLoc(uc.readOnSharedArray(enemyBaseLocCh) );
        enemyBaseFound = (uc.readOnSharedArray(enemyBaseFoundCh) == 1);

        baseInDanger = (uc.readOnSharedArray(baseDangerCh) == 1);

        inDungeon = (uc.senseVisibleTiles(TileType.DUNGEON).length > 0);

        //Unit specific update (maybe move out of updateGeneral)
        if(uc.getRound()%50 == 0) escapeDungeon = false;

    }

    void updateChannels() {
        unitReportCh     = x;       scoutReportCh         = 3 + x;    barbarianReportCh       = 6 + x;
        unitResetCh      = y;       scoutResetCh          = 3 + y;    barbarianResetCh        = 6 + y;
        unitCh           = z;       scoutCh               = 3 + z;    barbarianCh             = 6 + z;

        rangerReportCh   = 9 + x;   mageReportCh         = 12 + x;    knightReportCh         = 15 + x;
        rangerResetCh    = 9 + y;   mageResetCh          = 12 + y;    knightResetCh          = 15 + y;
        rangerCh         = 9 + z;   mageCh               = 12 + z;    knightCh               = 15 + z;

        assassinReportCh = 18 + x;  clericReportCh       = 21 + x;
        assassinResetCh  = 18 + y;  clericResetCh        = 21 + y;
        assassinCh       = 21 + z;  clericCh             = 24 + z;
    }

    void updateUnitCountInfo() {
        nUnits                   = uc.readOnSharedArray(unitCh);
        nScouts                  = uc.readOnSharedArray(scoutCh);
        nBarbarians              = uc.readOnSharedArray(barbarianCh);
        nRangers                 = uc.readOnSharedArray(rangerCh);
        nMages                   = uc.readOnSharedArray(mageCh);
        nKnights                 = uc.readOnSharedArray(knightCh);
        nAssassins               = uc.readOnSharedArray(assassinCh);
        nClerics                 = uc.readOnSharedArray(clericCh);
    }

    int saveShrine(Location loc) {
        int code = tools.encodeLoc(loc); //"1xxyy"
        int distance = Math.abs(allyBase.x - loc.x) + Math.abs(allyBase.y - loc.y);
        int shouldDestroy = 0;
        if (distance > shrineDistanceThreshold) shouldDestroy = 1;
        if (lastShrine > shrineCh[1]){
            uc.println("Can't save shrine at (" + loc.x + ", " + loc.y + ") with code: " + code + "\n");
            return 0;
        }
        uc.writeOnSharedArray(lastShrine, shouldDestroy + code*10); //"1xxyys"
        lastShrine = lastShrine + 1;
        return distance;
    }

    void saveChest (ChestInfo c){
        //TODO
    }
    int saveDungeon (Location loc){ //returns dungeon index, from 0 to 7
        for(int i = 0; i < 8; ++i){
            if(uc.readOnSharedArray(dungeonCh[i]) == 0){  //found a new dungeon!
                uc.writeOnSharedArray(dungeonCh[i], tools.encodeLoc(loc));
                uc.println("Adding element to dungeonArray: "+uc.readOnSharedArray(dungeonCh[0]) +","+ uc.readOnSharedArray(dungeonCh[1]) +","+ uc.readOnSharedArray(dungeonCh[2]) +","+ uc.readOnSharedArray(dungeonCh[3]) +","+ uc.readOnSharedArray(dungeonCh[4]) +","+ uc.readOnSharedArray(dungeonCh[5]) +","+ uc.readOnSharedArray(dungeonCh[6]) +","+ uc.readOnSharedArray(dungeonCh[7]));
                return i;
            }
            if(uc.readOnSharedArray(dungeonCh[i])%100000 == tools.encodeLoc(loc)){
                return i;
            }

        }
        uc.println("Exception in saveDungeon(dungeonLoc), probably found a ninth Dungeon.");
        return 0;
    }

}
