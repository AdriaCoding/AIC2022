package Murdak;

import aic2022.user.*;

public class Base {

    UnitController uc;
    Data data;
    Tools tools;
    public Base (UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
    }

    void run() {

        while (true) {

            //data.update();

            //report();

            spawnUnits();

            attack();

            attackN();

            uc.yield();
        }

    }


    void spawnUnits(){
        if (data.nUnit % 3 == 2) trySpawnBarbarian();
        else trySpawnRanger();
    }

    void trySpawnRanger() {
        boolean done = false;
        for (Direction dir : data.dirs) {
            if (!done && uc.canSpawn(UnitType.RANGER, dir)) {
                uc.spawn(UnitType.RANGER,dir);
                // Report to the Comm Channel
                uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh) + 1);
                //uc.write(data.rangerReportCh, uc.read(data.rangerReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                //uc.write(data.rangerResetCh, 0);
                // Update current data
                data.nUnit = data.nUnit + 1;
                //data.nRanger = data.nRanger + 1;
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
                //uc.write(data.rangerReportCh, uc.read(data.rangerReportCh) + 1);
                // Reset Next Slot
                uc.writeOnSharedArray(data.unitResetCh, 0);
                //uc.write(data.rangerResetCh, 0);
                // Update current data
                data.nUnit = data.nUnit + 1;
                //data.nRanger = data.nRanger + 1;
                done = true;
            }
        }
    }

    public void attack() {

        UnitInfo[] unitsAround = uc.senseUnits(data.enemyTeam);
        Location target = uc.getLocation();
        float priority = 0;

        for (UnitInfo unit : unitsAround) {

            int unitPriority = 1;

            if(!uc.canAttack(unit.getLocation())) continue;

            if (unitPriority > priority) {
                priority = unitPriority;
                target = unit.getLocation();
            }

        }
        if(!target.isEqual(uc.getLocation()) ) uc.attack(target);
    }

    public void attackN() {

        UnitInfo[] unitsAround = uc.senseUnits(Team.NEUTRAL);
        Location target = uc.getLocation();
        float priority = 0;

        for (UnitInfo unit : unitsAround) {

            int unitPriority = 1;

            if(!uc.canAttack(unit.getLocation())) continue;

            if (unitPriority > priority) {
                priority = unitPriority;
                target = unit.getLocation();
            }

        }
        if(!target.isEqual(uc.getLocation()) ) uc.attack(target);
    }

}
