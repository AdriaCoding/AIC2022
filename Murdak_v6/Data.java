package Murdak_v6;

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
    int scoutAccumulationRound = 10; //Round until we keep scout around the base
    int dungeonExplorationRound = 100; //Round where we start entering dungeons
    int shrineDistanceThreshold = 1800; //Distance at which we start destroying shrines

    int reinforcementDist = 4000; //Max distance to walk to enemyLoc
    int reinforcementRound = 150; //Round at which we start sending reinforcements

    int seekChestDist = 49; //max distance at which scouts go open chests
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

    // Dungeons info
    int[] dungeonCh = {45, 85}; int lastDungeon = dungeonCh[0];

    //GENERAL CHANNELS
    int accumulationCh = 100; // Ch 100
    int baseLocationCh = 101; // Ch 101
    int enemyFoundCh = 102;         int enemyLocCh = 103;   //Ch 102 & 103
    int enemyBaseFoundCh = 104;     int enemyBaseLocCh = 105;   //Ch 104 & 105
    int baseDangerCh = 106;

    //MAP CHANNELS - from 10000 to 18000

    //BFS CHANNELS - from 100000 to _
    int baseBFSCh = 100000;

    public Data (UnitController _uc) {
        uc = _uc;
        tools = new Tools(uc, this);
        ID = uc.getInfo().getID();
        type = uc.getType();
        allyTeam = uc.getTeam();
        enemyTeam = uc.getOpponent();
        currentRound = uc.getRound();

        //Explorer initialization
        prefDir = tools.randomDir();
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

        inDungeon = (uc.senseTileTypeAtLocation(uc.getLocation()) == TileType.DUNGEON);

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
    void saveDungeon (Location loc){
        //TODO
    }

}
