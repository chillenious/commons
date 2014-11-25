package com.chillenious.common.db.sync;

class Bam extends AbstractPersistentObject {

    final String name;

    final long oneToOneFk;

    Bam(long id, String name) {
        super(id);
        this.name = name;
        this.oneToOneFk = id * 10;
    }

    String getName() {
        return name;
    }

    long getOneToOneFk() { // for testing, simply the id times ten
        return oneToOneFk;
    }

    @Override
    public String toString() {
        return "Bam{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}
