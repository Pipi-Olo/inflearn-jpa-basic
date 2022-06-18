package jpashop.jpashop.domain.v5;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;

//@Entity
@DiscriminatorColumn
public class Album extends Item {

    private String artist;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
