package Murdak;

import aic2022.user.*;

public class Data {

    UnitController uc;
    Tools tools;

    Direction[] dirs = Direction.values();
    int ID;                     UnitType type;
    Location allyBase;          Location enemyBase;
    Team allyTeam;              Team enemyTeam;
    int currentRound;           int spawnRound;                 int turnsAlive;
    int nUnit;                  int nCombatUnit;
    int x;                      int y;                          int z;

    //CHANNELS
    int unitCh,             unitReportCh,           unitResetCh;            // Ch 0, 1, 2

    public Data (UnitController _uc) {
        uc = _uc;
        tools = new Tools(uc, this);
        ID = uc.getInfo().getID();
        type = uc.getType();
        allyTeam = uc.getTeam();
        currentRound = uc.getRound();
    }

    // This function is called once per turn
    public void update() {

        // General updates
        updateGeneral();
        updateChannels();
        //updateCommInfo();

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
        //Rw = allyTeam.getVictoryPoints();
        x = currentRound%3;
        y = (currentRound+1)%3;
        z = (currentRound+2)%3;
    }

    void updateChannels() {
        unitReportCh           = x;
        unitResetCh            = y;
        unitCh                 = z;

    }


}
