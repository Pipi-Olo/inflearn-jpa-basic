package jpashop.jpashop.domain.v9;

import javax.persistence.*;

@Entity
public class Member {

    @Id @GeneratedValue
    private Long id;

    private String name;

    // @Embedded or @Embeddable 하나만 넣어도 작동함
    // 둘다 작성 권장
    @Embedded
    private Period workPeriod;

    @Embedded
    private Address homeAddress;
}
