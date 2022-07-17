package Murdak_v8_old;

import aic2022.user.*;

public class Knight extends CombatUnit {

    public Knight(UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            report();

            attack();

            move();

            abilityOne();

            attack();

            getShrine();

            getChest();

            useArtifact();

            //enterDungeon();

            uc.yield();
        }

    }

    @Override
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.knightReportCh, uc.readOnSharedArray(data.knightReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.knightResetCh, 0);
    }

    void abilityOne(){

        UnitInfo[] units = uc.senseUnits(50,data.allyTeam, true);

        for (UnitInfo u : units){
            if ( (u.getType() == UnitType.RANGER || u.getType() == UnitType.MAGE)
                 && !uc.isObstructed(uc.getLocation(), u.getLocation() ) ){
                if(uc.getInfo().getLevel() < 2){
                    if(uc.canLevelUp() && uc.getReputation() > data.knightLvlThreshold ) uc.levelUp();

                    Direction dir = uc.getLocation().directionTo(u.getLocation());
                    Location loc = uc.getLocation().add(dir);
                    if( uc.canUseFirstAbility(loc) ) uc.useFirstAbility(loc);
                    return;
                }
                else{
                    Direction dir = uc.getLocation().directionTo(u.getLocation());
                    Location loc = uc.getLocation().add(dir);
                    if( uc.canUseFirstAbility(loc) ) uc.useFirstAbility(loc);
                    return;
                }

            }

        }

    }

}