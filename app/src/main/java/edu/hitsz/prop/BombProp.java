package edu.hitsz.prop;

import edu.hitsz.observer.GameObserver;
import edu.hitsz.observer.Subject;

import java.util.ArrayList;
import java.util.List;

public class BombProp extends AbstractProp implements Subject {
    private List<GameObserver> observers = new ArrayList<>();

    public BombProp(int locationX, int locationY, int speedX, int speedY){
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.updateOnBomb();
        }
    }
}
