package jpql;

import javax.persistence.*;
import java.util.List;

public class jpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
            Team teamA = new Team();
            teamA.setName("TeamA");
            em.persist(teamA);

            Team teamB = new Team();
            teamB.setName("TeamB");
            em.persist(teamB);

            Member member1 = new Member();
            member1.setName("Member1");
            member1.setAge(20);
            member1.changeTeam(teamA);
            em.persist(member1);

            Member member2 = new Member();
            member2.setName("Member2");
            member2.setAge(20);
            member2.changeTeam(teamA);
            em.persist(member2);

            Member member3 = new Member();
            member3.setName("Member3");
            member3.setAge(20);
            member3.changeTeam(teamB);
            em.persist(member3);

            em.flush();
            em.clear();

            int count = em.createQuery("update Member m set m.age = 30 where m.age = 20")
                    .executeUpdate();

            System.out.println("count = " + count);

            em.clear();

            Member findMember1 = em.find(Member.class, member1.getId());
            System.out.println("findMember1 = " + findMember1);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }

}
