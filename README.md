# inflearn-jpa-basic

# 🤔 SQL 개발의 문제점?
* 지루한 코드의 **무한 반복** → 기본 CRUD, 필드 변경시 모든 SQL 변경
* **패러다임의 불일치** (객체와 관계형 데이터베이스)
  * 상속
  * 연관관계
  * 객체 그래프 탐색
  * 비교
 
### 상속
객체와 다르게 테이블은 상속 기능이 없다. 가장 비슷한 기능이 테이블 슈퍼타입 / 서브타입 관계이다. 객체 하위 클래스를 저장하려면, 상위 클래스와 하위 클래스로 분리해서 INSERT 쿼리를 2번 날려야 한다.

### 연관관계
객체는 참조를 사용해서 연관된 객체를 조회한다. 테이블은 왜래키로 연관관계를 설정하고 조인으로 연관 테이블을 조회한다.

### 객체 그래프 탐색
객체는 자유롭게 객체 그래프를 탐색할 수 있다.
테이블은 처음 실행하는 SQL에 따라 탐색 범위가 결정된다.

```java
class Team {
  private Long id;
  
  private String name;
  private List<Member> members;
}

class Member {

  private Long id;
  private String username;
  private Team team;
}
```

```sql
SELECT m
FROM Member m
```
* 객체
  * member.getTeam().getName() : Member → Team 탐색 가능하다.
* 테이블
  * Member → Team 탐색 불가능하다.
  * 엔티티 신뢰 문제가 발생한다.
    * member.getTeam() : 값이 있는지 없는지 아무도 보장하지 않는다.
  * 모든 객체를 미리 로딩할 수는 없다.
    * Member만 조회하는 경우, Team∙Member 조회하는 경우 등 모든 상황에 맞는 SQL을 작성해야 한다.
  
### 비교
```java
Member member1 = "SELECT m FROM Member m WHERE m.id = 1"
Member member2 = "SELECT m FROM Member m WHERE m.id = 1"

member1 != member2
```
* 데이터베이스는 같은 PK로 조회했기 때문에 같은 값이지만,
* 객체는 각각 다른 인스턴스이기 때문에 다른 값으로 인식한다.

 
**객체를 자바 컬렉션에 저장하듯이 데이터베이스에 저장하는 방법 → JPA**

---

# 😲 JPA!
![](https://velog.velcdn.com/images/pipiolo/post/cb48bfa9-72ab-4094-9773-f6bfb4332d08/image.png)

* Java Persistence API, 자바 애플리케이션에서 관계형 데이터베이스 사용방법을 정의한 인터페이스
* 자바의 ORM 표준 기술
  * 객체 관계 매핑 (Object-Relational Mapping)
  * 객체는 객체답게, 데이터베이스는 데이터베이스 답계 설계
  * 그 차이를 **ORM**이 해결한다.
* 대표적인 구현체는 **Hibernate**가 있다.
* 자바 컬렉션을 사용하듯이 데이터베이스를 사용하는 것이 목표이다.
  
## 왜 JPA 인가?
* 생산성
* 패러다임 불일치 해결
* JPA 성능 최적화

### 생산성
* 메소드 1개로 기본 CRUD 반복 문제를 해결한다.
  * 저장 → `jpa.persist(member)`
  * 조회 → `jpa.find(memberId)`
  * 수정 → `member.setName("변경할 이름")`
  * 삭제 → `jpa.remove("member")`
* 엔티티 변경 시, 개발자는 객체 필드만 변경하면 된다. SQL은 JPA가 해결한다.

### 패러다임 불일치 해결
* 상속관계에 있는 객체를 저장 및 조회를 할 때, 추가적인 사항이 없다.
  * 개발자가 `jpa.persist(child)` 만 하면,
  * JPA가 `INSERT INTO Parent ...`, `INSERT INTO Child ...` 등 SQL을 해결한다.
* 연관관계 객체 탐색이 자유롭다.
  * 필요한 연관관계 객체를 JPA가 가져다준다.
  * 엔티티를 신뢰할 수 있다.
* 동일한 트랜잭션에서 조회한 엔티티는 동일성을 보장한다.

### JPA 성능 최적화
* 1차 캐시와 동일성 보장
  * 1차 캐시를 통해 같은 트랜잭션 안에서는 **같은 엔티티**를 반환한다.
  * 데이터베이스 격리 수준이 `Read Commit`이어도 애플리케이션은 `Repeatable Read`를 보장한다.
* 트랜잭션을 지원하는 쓰기 지연
  * 트랜잭션을 `Commit`할 때 까지 INSERT SQL을 모아둔다. JDBC BATCH SQL 기능을 사용해서 한 번에 모아둔 SQL을 전송한다.
  * UPDATE, DELETE로 인한 데이터베이스 `Lock`을 최소화한다. 트랜잭션 커밋할 때, UPDATE, DELETE SQL을 실행하고 바로 커밋한다.
  
## 데이터베이스 방언
![](https://velog.velcdn.com/images/pipiolo/post/7a722782-62a3-47e8-9404-adb62899422b/image.png)

* `JPA`는 **특정 데이터베이스에 종속하지 않는다.**
* 각각의 데이터베이스가 제공하는 SQL 문법과 함수는 다르다.
  * 페이징 : MySQL → LIMIT, Oracle → ROWNUM
  * 가변 문자 : MySQL → VARCHAR, Oracle → VARCHAR2
* SQL 표준을 지키지 않는 특정 데이터베이스만의 고유한 기능을 해결한다.

## JPQL
* 테이블이 아닌 객체를 대상으로 검색하는 **객체 지향 쿼리**이다.
  * `JPQL`은 엔티티 객체를 대상으로, `SQL`은 데이터베이스 테이블을 대상으로 작동한다.
* JPA는 SQL을 추상화한 `JPQL` 객체 지향 쿼리 언어를 제공한다.
  * SQL을 추상화했기 때문에 **특정 데이터베이스 SQL에 의존하지 않는다.**
  * 데이터베이스 방언을 사용해 `JPQL`을 통해 `SQL`을 만든다.

---
