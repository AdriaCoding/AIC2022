package Murdak_v8_old;

import aic2022.user.*;

public class Base extends Unit {

    public Base (UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            report();

            spawnUnits();

            attack();

            getShrine();

            if (uc.getRound() == 1) reportMyself();

            if (uc.getRound() == 1) senseStuff();

//            int i = data.baseBFSCh;
//            boolean found = false;
//            String content = "";
//            while(!found){
//                if (uc.readOnSharedArray(i) == 0) found = true;
//                else content = content + Integer.toString(uc.readOnSharedArray(i)) + ", ";
//                i += 1;
//            }
//            if (!content.equals("")) uc.println("["+content +"] round = " + uc.getRound());
//            if (uc.getRound() == 2){
//                uc.println("La base està a " + tools.encodeLoc(data.allyBase));
//                for (int i =0; i <9 ; ++i){
//                    uc.println("La direcció " + tools.dirsBFS[i] + " torna " + tools.encodeLoc(data.allyBase.add(tools.dirsBFS[i])));
//                }
//            }
            if (uc.getRound() > 4) tools.baseBFS(tools.encodeLoc(data.allyBase), data.baseBFSCh); //falla a la ronda 2 por algún misterioso motivo

            uc.yield();
        }

    }

    void report(){
        resetDynamicChannels();
        reportDanger();
        reportAccumulationLocation();
        reportEnemies();
    }

    void resetDynamicChannels(){
        //Every 20 rounds we reset some channels to ensure we are considering new objectives.
        if(uc.getRound()%20 != 0) return;

        //Enemy last known location
        uc.writeOnSharedArray(data.enemyLocCh, 0);
        uc.writeOnSharedArray(data.enemyFoundCh, 0);
        data.enemyFound = false;

        //Base danger indicator
        uc.writeOnSharedArray(data.baseDangerCh, 0);
        data.baseInDanger = false;

    }

    void reportDanger(){
        if(!data.baseInDanger){
            UnitInfo[] enemies = uc.senseUnits(64,data.allyTeam,true);
            if(enemies.length > 4 ){
                uc.writeOnSharedArray(data.baseDangerCh, 1);
                data.baseInDanger = true;
            }
        }
    }

    void reportAccumulationLocation(){

        if(uc.getRound()%20 != 0) return;

        Location[] cells = uc.getVisibleLocations(49);

        for(int i = 0; i < 10; ++i){
            //select a random visible cell
            Location target = cells[ tools.randomInt(cells.length) ];
            if(!uc.isOutOfMap(target) && !uc.isObstructed(uc.getLocation(),target)){
                uc.writeOnSharedArray( data.accumulationCh, tools.encodeLoc(target));
                return;
            }
        }
        uc.writeOnSharedArray( data.accumulationCh,tools.encodeLoc(uc.getLocation()));
    }

    void reportMyself(){
        Location myLoc = uc.getLocation();
        uc.writeOnSharedArray(data.baseLocationCh, tools.encodeLoc(myLoc));
    }

    void spawnUnits(){
        //TODO millorar spawn rates dels diferents tipus de unitats

        if (data.nUnits % 5 == 0 && data.nScouts < 3) trySpawnExplorer();
        else if (data.nUnits % 3 == 1) trySpawnBarbarian();
        else if (data.nUnits % 3 == 2) trySpawnKnight();
        else trySpawnRanger();
    }

    void trySpawnRanger() {
        boolean done = false;
        for (Direction dir : data.dirs) {
            if (!done && uc.canSpawn(UnitType.RANGER, dir)) {
                uc.spawn(UnitType.RANGER,dir);
                // Report to the Comm Channel
                uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh) + 1);
                uc.writeOnSharedArray(data.rangerReportCh, uc.readOnSharedArray(data.rangerReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                uc.writeOnSharedArray(data.rangerResetCh, 0);
                // Update current data
                data.nUnits = data.nUnits + 1;
                data.nRangers = data.nRangers + 1;
                done = true;
            }
        }
    }

    void trySpawnBarbarian() {
        boolean done = false;
        for (Direction dir : data.dirs) {
            if (!done && uc.canSpawn(UnitType.BARBARIAN, dir)) {
                uc.spawn(UnitType.BARBARIAN,dir);
                // Report to the Comm Channel
                uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh) + 1);
                uc.writeOnSharedArray(data.barbarianReportCh, uc.readOnSharedArray(data.barbarianReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                uc.writeOnSharedArray(data.barbarianResetCh, 0);
                // Update current data
                data.nUnits = data.nUnits + 1;
                data.nBarbarians = data.nBarbarians + 1;
                done = true;
            }
        }
    }
    void trySpawnExplorer() {
        boolean done = false;
        for (Direction dir : data.dirs) {
            if (!done && uc.canSpawn(UnitType.EXPLORER, dir)) {
                uc.spawn(UnitType.EXPLORER,dir);
                // Report to the Comm Channel
                uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh) + 1);
                uc.writeOnSharedArray(data.scoutReportCh, uc.readOnSharedArray(data.scoutReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                uc.writeOnSharedArray(data.scoutResetCh, 0);
                // Update current data
                data.nUnits = data.nUnits + 1;
                data.nScouts = data.nScouts + 1;
                done = true;
            }
        }
    }
    void trySpawnKnight() {
        boolean done = false;
        for (Direction dir : data.dirs) {
            if (!done && uc.canSpawn(UnitType.KNIGHT, dir)) {
                uc.spawn(UnitType.KNIGHT,dir);
                // Report to the Comm Channel
                uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh) + 1);
                uc.writeOnSharedArray(data.knightReportCh, uc.readOnSharedArray(data.knightReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                uc.writeOnSharedArray(data.knightResetCh, 0);
                // Update current data
                data.nUnits = data.nUnits + 1;
                data.nKnights = data.nKnights + 1;
                done = true;
            }
        }
    }

}