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
            Team team = new Team();
            team.setName("Team");
            em.persist(team);

            Member member = new Member();
            member.setName("Team");
            member.setAge(20);
            member.changeTeam(team);
            em.persist(member);

            em.flush();
            em.clear();

            String jpql = "select m from Member m left join Team t on m.name = t.name";
            List<Member> result = em.createQuery(jpql, Member.class)
                    .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }

}
