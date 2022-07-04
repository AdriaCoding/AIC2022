package Murdak;

import aic2022.user.*;

public class Unit {

    UnitController uc;
    Data data;
    Tools tools;

    public void attack(){

        UnitInfo[] enemiesAround = uc.senseUnits(data.enemyTeam);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

    public void attackN(){

        UnitInfo[] enemiesAround = uc.senseUnits(Team.NEUTRAL);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

    public int targetPriority(UnitInfo unit) {
        if(unit.getType() == UnitType.CLERIC)       return 8;
        if(unit.getType() == UnitType.ASSASSIN)     return 7;
        if(unit.getType() == UnitType.MAGE)         return 6;
        if(unit.getType() == UnitType.EXPLORER)     return 5;
        if(unit.getType() == UnitType.RANGER)       return 4;
        if(unit.getType() == UnitType.BARBARIAN)    return 3;
        if(unit.getType() == UnitType.KNIGHT)       return 2;
        if(unit.getType() == UnitType.BASE)         return 1;

        return 0;
    }

}
