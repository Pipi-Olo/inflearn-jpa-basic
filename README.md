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

# 영속성 컨텍스트❗
* 엔티티를 영구적으로 저장하는 환경을 제공한다.
* `EntityManager`를 통해 영속성 컨텍스트에 접근한다.
* `EntityManager.persist(member)` → **데이터베이스에 저장하는 것이 아니다.** 영속성 컨텍스트에 저장하는 것이다.

## 엔티티 생명주기
![](https://velog.velcdn.com/images/pipiolo/post/2f8098db-bfb6-4afa-864d-ce2d0b035976/image.png)

* 비영속 (new / transient)
  * 영속성 컨텍스트와 관계가 없는 상태
* 영속 (managed)
  * `em.persist(member)`
  * 영속성 컨텍스트에 관리되는 상태
* 준영속 (detached)
  * `em.detach(member)`
  * 영속성 컨텍스트에 저장된 엔티티를 분리한 상태
* 삭제 (removed)
  * `em.remove(member)`
  * 엔티티를 삭제한 상태
  
## 영속성 컨텍스트 이점
* 1차 캐시
* 동일성 보장 (Identity)
* 트랜잭션을 지원하는 쓰기 지연 (Transaction Write-Behind)
* 변경 감지 (Dirty Checking)
* 지연 로딩 (Lazy Loading)

### 1차 캐시
![](https://velog.velcdn.com/images/pipiolo/post/61079587-e9f3-4472-a5e1-0f53631a0b8d/image.png)

* **영속성 컨텍스트의 물리적 위치**
* 식별자(`@Id`)를 통해서 엔티티를 식별한다.
  * 영속되기 위해서는 식별자가 반드시 필요하다.
* `em.find(memberId)`
  * 데이터베이스에서 직접 조회하는 것이 아니다. **1차 캐시에서 조회한다.**
  * 1차 캐시에 없는 경우, 데이터베이스를 조회해서 1차 캐시에 저장한다. 결국 1차 캐시에서 엔티티를 조회한다.
  
> **참고**
> 애플리케이션 전체에서 공유하는 캐시를 `2차 캐시`라 한다.
  
### 동일성 보장
```java
Member member1 = em.find(memberId);
Member member2 = em.find(memberId);

member1 == member2 // true
```
* 1차 캐시에서 엔티티를 읽어오기 때문에 동일성을 보장한다.
  * 동일성 (Identity) → 두 객체가 완전히 같다는 뜻으로 주소 값이 같다.
  * 동등성 (Equality) → 두 객체가 같은 정보를 갖고 있다는 뜻으로 `equals()` 메소드를 통해 판단한다.
* 반복 가능한 읽기(Repeatable Read) 트랜잭션 격리 수준이 데이터베이스가 아닌 애플리케이션 차원에서 제공된다.

### 트랜잭션을 지원하는 쓰기 지연
![](https://velog.velcdn.com/images/pipiolo/post/be8378f3-f321-4c10-95f7-9ec4ecad9314/image.png)

* `em.persist(memberA)`
  * 데이터베이스에 저장하지 않고 영속성 컨텍스트에 저장한다.
  * `INSERT SQL`을 생성해서 쓰기 지연 SQL 저장소에 모아둔다.
* `transaction.commit()`
  * 트랜잭션 커밋이 발생하면 모아둔 SQL들을 데이터베이스에 반영한다.
* 데이터베이스에 접근하는 횟수를 줄어 성능이 향상된다.

### 변경 감지
![](https://velog.velcdn.com/images/pipiolo/post/02b9c20a-7b20-4c34-abe2-4a8a209fde2d/image.png)

```java
transaction.begin();

Member member = em.find(memberId);

member.setName("Hello JPA");
member.setAge(25);

transaction.commit(); // 커밋하면 내부적으로 플러쉬가 발생한다.
```

* 엔티티가 영속되는 순간에 스냅샷을 저장한다.
* `flush()`가 발생하면 현 엔티티와 스냅샷을 비교해서 차이점을 찾는다.
* 차이점이 있으면 UPDATE SQL을 생성해서 쓰기 지연 SQL 저장소에 저장한다.
* 데이터베이스에 반영한다.
* `em.update(member)` 코드가 없어도 엔티티를 변경하면 플러시 시점에 자동으로 데이터베이스에 반영된다.

### 지연 로딩
> 지연 로딩에 대한 설명은 뒤에서 진행하겠다.

## 플러시
* 영속성 컨텍스트의 변경내용을 데이터베이스에 동기화한다.
* `flush()`를 호출하려면
  * `em.flush()` → 플러시 직접 호출
  * 트랜잭션 커밋 → 플러시 자동 호출
  * JPQL 쿼리 실행 → 플러시 자동 호출
* `flush()`가 발생하면
  * 엔티티들의 변경을 감지한다.
  * 생성된 SQL을 `쓰기 지연 SQL 저장소`에 등록한다.
  * 저장소에 모아둔 SQL을 데이터베이스에 전송한다.
* **영속성 컨텍스트는 그대로 있다.** 변경되거나 삭제되지 않는다.
* 커밋 직전에만 동기화하면 되기 때문에 트랜잭션 단위가 중요하다.
* 영속 혹은 삭제 상태 엔티티만 데이터베이스에 반영된다.
  * 준영속 상태 엔티티는 영속성 컨텍스트가 제공하는 혜택을 못 받는다.
  * `em.detach(entity)`, `em.clear()`, `em.close()`

---

# 엔티티 매핑 ✓
* 객체와 테이플 매핑 : `@Entity`, `@Table`
* 필드와 컬럼 매핑 : `@Column`
* 기본키 매핑 : `@Id`
* 연관관계 매핑 : `@ManyToOne`, `@JoinColumn`

## 객체와 테이플 매핑
### @Entity
* `@Entity`가 붙은 클래스를 JPA가 관리하며, 엔티티라 한다.
* 기본 생성자가 필수이다.
* final, inner 클래스, enum, interface는 엔티티가 불가능하다.
* final 필드는 저장되지 않는다.
* 속성
  * name
    * JPA 에서 사용할 **엔티티 이름**을 지정한다. 테이블 이름과 무관하다.
    * 기본 값은 클래스 이름이다.

### @Table
* 엔티티와 매핑할 테이블을 지정한다.
* 속성
  * name
    * 매핑할 테이블 이름일 지정한다.
    * 기본 값은 엔티티 이름이다.
  * catalog
  * schema
  * uniqueConstraints(DDL)
    * DDL 생성 시, 유니크 제약 조건을 생성한다.
    
> 데이터베이스 스키마 자동 생성 (Data Definition Language)
> `DDL`이란 데이터 생성, 수정, 삭제 등 데이터 전체의 골격을 결정하는 역할을 지닌, 데이터베이스를 정의하는 언어이다. 데이터베이스 방언을 참고해 애플리케이션 실행 시점에 자동으로 생성한다. DDL은 애플리케이션 실행 시점에만 사용되고 JPA 실행 로직에는 영향을 주지 않는다.
> ```java
> <property name="hibernate.hbm2ddl.auto" value="..."/>
> 
> * create : 기존 테이블 삭제 후 생성한다. (drop-create)
> * create-drop : 종료 시점에 테이블을 삭제한다.
> * update : 변경된 내용만 반영한다. 데이터가 유지된다.
> * validate : 엔티티-테이블 매핑이 정상인지 확인한다.
> * none : 아무것도 안 한다.
> ```
> * 개발 서버 : create 또는 update
> * 테스트 서버 : update 또는 validate
> * 운영 서버 : validate 또는 none

## 필드와 컬럼 매핑
```java
 @Entity
 public class Member {
   
   @Id
   private Long id;
   
   @Column(name = "name")
   private String username;

   private Integer age;
 
   @Enumerated(EnumType.STRING)
   private RoleType roleType;
 
   @Temporal(TemporalType.TIMESTAMP)
   private Date createdDate;
 
   @Temporal(TemporalType.TIMESTAMP)
   private Date lastModifiedDate;
 
   @Lob
   private String description;
}
```

* @Column → 컬럼을 매핑한다.
* @Enumerated → enum 타입 매핑한다.
* @Temporal → 날짜 타입을 매핑한다.
* @Lob → BLOB, CLOB 매핑한다.
* @Transient → 특정 필드를 매핑하지 않는다.
* 엔티티 클래스 필드에 매핑을 생략하면 @Column이 적용된다.

### @Column
* 해당 필드를 컬럼에 매핑한다.
* 속성
  * name
    * 필드와 매핑할 테이블 컬럼의 이름을 지정한다.
    * 객체의 필드 이름이 기본 값이다.
  * insertable / updatable
    * 등록, 변경 가능 여부를 결정한다.
    * `true`가 기본 값이다.
  * nullable (DDL)
    * null 값의 적용 여부를 결정한다.
    * `false`이면 해당 컬럼에 not null 제약 조건이 붙는다.
  * unique (DDL)
    * 유니크 제약 조건을 걸 때 사용한다.
    * 일반적으로 `@Table`의 uniqueConstraints를 사용한다.
  * columnDefinition (DDL)
    * 데이터베이스 컬럼 정보를 직접 줄 수 있다.
  * length (DDL)
    * 문자 길이 제약 조건을 추가한다.
    * `String` 타입에만 적용 가능하다.
    * 기본 값은 255이다.
  * precision, sacle (DDL)
    * `BigDecimal` 타입에 사용한다.
    * 아주 큰 숫자나 정밀한 소수를 다룰 때 사용한다.
    * 기본 값은 precision = 19, scale = 2이다.

### @Enumerated
* 자바 enum 타입을 매핑할 때 사용한다.
* 속성
  * value
    * EnumType.ORDINAL → enum 순서(1, 2, 3 ...) 값을 데이터베이스에 저장한다.
    * EnumType.STRING → enum 이름을 데이터베이스에 저장한다.
    * 기본 값은 ORDINAL이지만, 무조건 `STRING` 사용한다.
    
> EnumType.ORDINAL
> enum 순서가 변경되어도, 이미 저장된 데이터들의 값이 변경되지 않는다. 모든 데이터의 값을 변경해야 하는 문제가 발생한다. `EnumType.STRING`을 사용하자.
    
### @Temporal
* 날짜 타입(Date, Calendar)을 매핑할 때 사용한다.
* 속성
  * value
    * TemporalType.DATE → 날짜를 매핑한다. 예) 2013-10-11
    * TemporalType.TIME → 시간을 매핑한다. 예) 11:14:34
    * TemporalType.STAMP → 날짜와 시간을 매핑한다. 예) 2013-10-11 11:14:34

> LocalDate, LocalDateTime 사용할 때는 `@Temporal` 생략한다.
> LocalDate → TemporalType.DATE
> LocalDateTime → TemporalType.STAMP 자동 매핑된다.

### @Lob
* 데이터베이스 CLOB, BLOB 타입과 매핑된다.
* `@Lob`는 속성이 없다.
* 매핑하는 필드 타입이 문자면 CLOB 매핑, 나머지는 BLOB 매핑이 된다.

### @Transient
* 해당 필드를 매핑하지 않는다.
* 해당 필드는 데이터베이스에 저장되지 않는다.
* 메모리에서 임시로 값을 보관할 때 사용한다.

## 기본키 매핑
```java
@Id @GeneratedValue(strategy = GenerationType.XXX)
private Long id;
```
* `@Id` → 엔티티 식별자, 테이블 PK로 사용된다.
* `@GeneratedValue` → 기본 키를 자동으로 생성한다.
  * IDENTITY → 데이터베이스에 위임한다. ex) `MySQL`
  * SEQUENCE → 데이터베이스 시퀀스 오브젝트를 사용한다. ex) `Oracle`
  * AUTO → 데이터베이스 방언에 따라 `IDENTITY`, `SEQUENCE` 중 하나를 자동으로 지정한다. 기본 값이다.
  * TABLE → 키 생성 전용 테이블을 생성한다. 해당 키 생성 테이블은 데이터베이스 내 모든 테이블에서 사용한다.
  
### IDENTITY 전략
* 기본 키 생성을 데이터베이스에 위임한다.
* MySQL, PostgreSQL, SQL Server 에서 사용된다.
* JPA는 `transaction.commit()` 시점에 `INSERT SQL`을 실행한다.
* 하지만, `IDENTITY` 전략은`em.persist(entity)` 시점에 즉시 `INSERT SQL` 실행하고 데이터베이스에서 식별자를 조회한다. 
  * 영속성 컨텍스트가 엔티티를 관리하기 위해서는 식별자(`@Id`)가 필요하다.
  * `@Id` 생성을 데이터베이스에게 위임했으니, 데이터베이스로부터 식별자를 받아야 영속성 컨텍스트에서 엔티티 관리가 가능하다.
  * 유의미한 성능 차이는 없다.

### SEQUENCE 전략
```java
@Entity
@SequenceGenerator(
    name = "MEMBER_SEQ_GENERATOR",
    sequenceName = "MEMBER_SEQ",
    initialValue = 1,
    allocationSize = 1)
public class Member {

  @Id 
  @GeneratedValue(strategy = GenerationType.SEQUENCE,
          generatir = "MEMBER_SEQ_GENERATOR")
  private Long id;
}
```

* 데이터베이스 시퀀스는 유일한 값을 순서대로 생성하는 데이터베이스 기술이다.
* Orcle, H2 Database 에서 사용된다.
* `@SequenceGenerator`가 필요하다.
  * 엔티티를 생성할 때, `Sequence`를 조회하는 쿼리를 보내서 `id` 값을 얻고 매핑한다.
  * `allocationSize` → 쿼리를 통해 한 번 `id` 값을 얻을 때, 가져올 개수를 말한다.
    * 기본 값(`50`)일 경우, `MEMBER_SEQ_1` ~ `MEMBER_SEQ_50`까지 한 번에 식별자를 얻어온다.
    * 51번째 식별자가 필요할 때, `MEMBER_SEQ_51` ~ `MEMBER_SEQ_100`까지 얻는다.
* `IDENTITY`전략과 다르게 `SELECT SQL`을 통해 식별자를 얻는다.
* 하나의 데이터베이스에 동시에 여러 접근을 해도 문제없다.

### TABLE 전략
* 키 생성 전용 테이블을 만들어 데이터베이스 시퀀스 전략을 흉내낸다.
* 하나의 테이블로 모든 테이블의 식별자 문제를 해결가능하다.
* <span style="color: #FF8C00">성능이 좋지 않다. 쓰지 말자.</span>

### Long + 대체키 + 키 생성 전략❗
* 기본 키 제약 조건
  * NULL ❌
  * 유일해야 한다.
  * 변하면 안 된다.
* 미래에도 이 조건을 만족하는 자연키는 없다. 대체키를 사용하자.
* 비지니스 변화에 유연하게 대처할 수 있다.
* 주민등록번호도 적절하지 않다.
  * 주민등록번호 관련 법률, 정책 변화로 인해 미래에 어떻게 변화할지 아무도 모른다.

### 연관관계 매핑

> 연관관계 매핑에 대한 설명은 다음 장에서 진행하겠다.

---

# 연관관계 매핑
## 객체 지향 모델링
```java
@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
  
  private Team team     // 객체 지향 모데링
  private Long team_id; // 테이블 중심 모델링
}

Member member = em.find(member.getId());
Team team = em.find(member.getTeamId()); // Team team = member.getTeam();
```

* 객체와 테이블간의 패러다임 차이를 해결해야 한다.
  * 객체는 참조를 사용해서 연관된 객체를 찾는다.
  * 테이블은 외래 키 조인을 사용해서 연관된 테이블을 찾는다.
* 객체를 테이블 중심 모델링을 하면, 자바 컬렉션 처럼 `member.getTeam()`으로 데이터에 접근할 수 없다.
* 개발자는 `객체 지향 모델링`을 사용하자. 테이블에 대한 외래키 조인은 JPA가 해결한다.

## 연관관계 매핑 고려사항 3가지
* 방향 → `단방향`, `양방향`
  * 테이블 : 단방향 개념이 없다. 외래 키 조인 하나로 양방향으로 조회 가능하다. (A ↔ B)
  * 객체 : 양방향 개념이 없다. 단방향 2개가 있을 뿐이다. (A → B, A ← B)
* 다중성 → `일대다 (1:N)`, `다대일 (N:1)`, `일대일 (1:1)`, `다대다 (N:M)`
  * `일대다`는 `일`을 연관관계 주인으로,
  * `다대일`은 `다`를 연관관계 주인으로 설정한다는 뜻이다.
* 연관관계 주인
  * 객체 양방향 연관관계는 테이블의 외래 키를 관리하는 `주인`을 정해야 한다.
  * 주인이 아닌 엔티티는 외래 키에 영향을 주지 않는다. 단순 조회만 가능하다.

## 방향
### 객체와 테이블의 양방향 연관관계 패러다임 차이
![](https://velog.velcdn.com/images/pipiolo/post/047addef-610d-43ac-aa7a-e4b3bd17933d/image.png)


* 객체의 양방향 관계는 사실 **서로 다른 단방향 관계 2개**이다.
  * 객체 연관관계 2개
    * 회원 → 팀 : 단방향 연관관계 1개, `member.getTeam()`
    * 팀 → 회원 : 단방향 연관관게 1개, `team.getMembers()`
  * 양방향 연관관계 없다.
* 테이블은 **외래 키 하나**로 앙방향 연관관계를 가진다.
  * 테이블 연관관계 1개
    * 회원 ↔ 팀 양방향 연관관계 1개
    * 외래 키 하나로 테이블 양쪽으로 조인할 수 있다.
  * 단방향 연관관계 없다.

> **참고**
> `Member`가 속한 `Team`을 변경할 때, 
> `Team`에서 `Member`를 추가해야 할 까? 아니면 `Member`에서 `Team`을 변경해야 할 까?
> **두 객체 중 하나를 양방향 연관관계의 주인으로 지정해야 한다.**

## 양방향 연관관계의 주인 (Owner)
```java
@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne
  private Team team;
}

@Entity
public class Team {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "team")
  private List<Member> members = new ArrayList<>();
}
```

* **외래 키를 가지고 있는 테이블이 `주인`이다.**
  * Member 테이블에 TEAM_ID 외래 키가 있으므로 Member가 `주인`이다.
  * 일대다(1:N), 다대일(N:1) 관계에서 주인은 항상 `다(N)`이다.
  * 연관관계 주인은 비지니스와 연관이 없다. 오직 외래 키 여부로 결정한다.
* 연관관계의 주인만이 엔티티를 등록 및 수정할 수 있다.
* 주인이 아닌 엔티티는 읽기만 가능하다.
  * 주인이 아닌 엔티티는 `mappedBy` 속성으로 주인을 지정한다.
    * `(mappedBy = "team)"`은 `Member.team`이 주인이라는 뜻이다.
  * `team.getMembers().add(member)` → 역방향(주인이 아닌 방향) 연관관계 설정은 데이터베이스에 반영되지 않는다.

![](https://velog.velcdn.com/images/pipiolo/post/f197b32f-746f-4ba7-a8f3-b785c603c8b8/image.png)

```java
@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne
  private Team team;
  
  // 연관관계 편의 메소드
  public void changeTeam(Team team) {
    this.team = team;
    team.getMembers().add(this);
  }
}

@Entity
public class Team {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "team")
  private List<Member> members = new ArrayList<>();
}
```

* 객체 단방향 매핑만으로도 테이블 연관관계 매핑은 완료되었다.
  * **테이블에 영향을 주지 않기 때문에**, 객체 양방향 매핑은 필요할 때 추가하면 된다.
  * 테이블은 외래 키 하나로 양방향 연관관계 매핑을 완료했기 때문이다.
* 객체 양방향 매핑은 객체 그래프 역방향 조회 기능이 추가된 것 뿐이다.
* 객체 양방향 매핑 시, 항상 양쪽에 값을 설정한다.
  * 연관관계 `편의 메소드`를 사용한다.
  * `team.getMembers().add(this)`는 데이터베이스에 반영되지 않는다. 하지만, 자바의 객체 관점에서 추가하는 것이 좋다.
* 객체 양방향 매핑 시, 무한 루프를 주의한다.
  * toString(), @Lombok → `toString()` 메소드에서 Member ↔ Team 서로를 계속해서 호출하는 무한 루프 오류가 발생한다.
  * JSON 생성 라이브러리 → `Controller`에서 엔티티 반환 시, JSON 자동 변환기에서 무한 루프 오류가 발생한다.  
    * `Controller`는 엔티티를 반환하지 않는다. `Controller`는 항상 DTO를 반환한다.
    * `DTO`는 무한 루프가 발생하지 않도록 단방향 관계로 설계한다.
* **처음에는 단방향 관계만 설정한다. 양방향 관계는 필요할 때 추가한다. 테이블에 영향을 주지 않으니 문제없다.**

## 다중성
### 다대일 (N : 1), @ManyToOne
외래 키를 가진 `다` 엔티티가 연관관계의 `주인`이다.

```java
@Entity
public class Team {

  @Id @GeneratedValue
  private Long id;
  
  @OneToMany(mappedBy = "team") // Member.team 이 주인이라는 뜻이다.
  private List<Member> members = new ArrayList<>();
}

@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "team_id")
  private Team team;
}
```

* 다대일 단방향 → 가장 많이 사용하는 연관관계 매핑 방법이다. 
* 다대일 양방향 → 연관관계 `편의 메소드`를 만들어 주자.
  * 주인이 아닌 `Team`은 조회만 가능하다. `team.getMembers.addMember(member)`는 데이터베이스에 반영되지 않는다.
  * 무한 루프를 주의하자.

### 일대다 (1 : N), @OneToMany
실제 외래키는 `다` 테이블이 가지고 있지만, `일`을 외래 키를 관리하는 주인으로 설정한 매핑이다.
`다대일` 연관관계 매핑을 사용하자.

```java
@Entity
public class Team {

  @Id @GeneratedValue
  private Long id;

  @OneToMany
  @JoinColumn(name = "team_id")
  private List<Member> members = new ArrayList<>();
}

@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
}
```
* 일대다 단방향 → `다대일 양방향`을 사용하자.
  * 패러다임 차이때문에 반대편 테이블이 외래 키를 관리하는 특이한 구조
    * 엔티티가 관리하는 참조가 다른 테이블의 외래 키에 있다.
    * 연관관계 관리를 위한 추가 `UPDATE SQL`이 실행된다.
  * `@JoinColumn`이 필수이다.
    * 생략할 경우 중간에 테이블을 하나 추가하는 조인 테이블 방식을 사용한다.
  * 반드시 `Team`이 `Member`를 관리해야 한다면 `다대일 양방향`을 사용하자.
* 일대다 양방향 → 존재하지 않는 매핑이다. `다대일 양방향`을 사용하자.
  * 존재하지 않는 매핑 방법이지만 반드시 사용해야한다면, `@JoinColumn(insertable=false, updatable=false)` 설정하면 된다.

### 일대일 (1 : 1), @OneToOne
`주 테이블`이나 `대상 테이블` 중 외래 키를 선택할 수 있다. 어차피 그 반대도 일대일 관계이기 때문이다.
외래 키에 데이터베이스 유니크 제약조건을 추가하자.

```java
@Entity
public class User {

    @Id @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "locker_id")
    private Locker locker;   
}

@Entity
public class Locker {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "locker")
    private User user;                          
}
```

* 일대일 단방향 → `JPA`가 지원하지 않는 매핑방법이다. 불가능하다.
* 일대일 양방향 → `다대일 양방향` 매핑과 유사하다. 외래 키가 있는 곳이 연관관계의 주인이다. 주인의 반대편은 `mappedBy`를 적용한다.
  * 주 테이블(`User`)에 외래 키
    * 주 객체가 대상 객체의 참조를 가지고 있는 것처럼, 주 테이블이 외래 키를 가지고 있다.
    * 객체지향 개발자 선호한다.
    * JPA 매핑 개념과 같다.
    * 장점 : 주 테이블만 조회해도 대상 테이블에 데이터가 있는지 확인 가능하다.
    * 단점 : 값이 없으민 외래키 `NULL`값이 허용된다.
  * 대상 테이블(`Locker`)에 외래 키, 
    * 대상 테이블에 외래키가 존재한다.
    * 데이터베이스 개발자가 선호한다.
    * 장점 : 일대다 관계로 변경해도 테이블 구조를 유지할 수 있다.
    * 단점 : 프록기 기능 한계로 항상 즉시 로딩으로 작동한다. 지연 로딩으로 설정해도 즉시 로딩으로 실행된다.

### 다대다 (N : M), @ManyToMany
<span style="color: #FF8C00">실무에서 사용하지 말자.</span>
`다대다` 관계는 정규화 테이블 2개로 표현할 수 없다. `연결 테이블`을 추가해서 일대다, 다대일 관계로 해결해야 한다.

```java
@Entity
public class Member {

    @Id @GeneratedValue
    private Long id;
    
    @ManyToMany
    @JoinTable(name = "member_product",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products = new ArrayList<>();
}

@Entity
public class Product {

    @Id @GeneratedValue
    private Long id;
    
    @ManyToMany(mappedBy = "products")
    private List<Member> members = new ArrayList<>();
}

```

* `Member.products`가 연관관계 주인이다.
* `@JoinTable`로 추가할 연결 테이블을 지정한다.
  * name → 연결 테이블 이름을 지정한다.
  * joinColumns → 연관관계 주인 `PK`를 지정한다.
  * inverseJoinColumns → 주인 반대편 `PK`를 지정한다.
  * `joinColumns`, `inverJoinColumns`을 합쳐서 새로운 테이블의 `(PK, FK)`가 된다.
  
#### 다대다 매핑의 한계
* 실무에서는 사용하지 않는다.
* 연결 테이블이 단순 연결만 하고 끝나지 않는다.
  * 연결 테이블에 다른 데이터가 들어갈 수 없다.
  * `member_id`, `product_id`이외 다른 Column을 허용하지 않는다.

#### 다대다 한계 극복
![](https://velog.velcdn.com/images/pipiolo/post/eff0cc1e-074b-4c6f-a058-b5de6f554d7f/image.png)

```java
@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "member")
  private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {

  @Id @GeneratedValue
  private Long id;

  @ManyToOne
  @JoinColumn(name = "member_id")
  private Member member;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;
}

@Entity
public class Product {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "product")
  private List<Member> members = new ArrayList<>();
}
```

* 연결 테이블(`Order`)을 엔티티로 승격시킨다.
* `@ManyToMany` → `@OneToMany` + `@ManyToOne`
* 다른 엔티티처럼 `Long + 대체키 + 키 생성 전략 ❗`을 통해 테이블 PK를 설정하자.
* 연관관계의 주인은 연결 테이블(`Order`)이다.
  * `(mappedBy = "member")` → `Order.member`가 연관관계 주인이다.
  * `(mappedBy = "product")` → `Order.product`가 연관관계 주인이다.

--- 

# 고급 매핑
## 상속관계 매핑
* 관계형 데이터베이스는 상속 관계가 없다.
* 객체의 상속은 데이터베이스의 슈퍼타입 / 서브타입 논리 모델링 기법으로 매핑한다.
  * `@Inheritance(strategy = InheritanceType.XXX)` → 물리 모델로 구현하는 방법 3가지
    * `JOINED` : 조인 전략 
    * `SINGLE_TABLE` : 단일 테이블 전략
    * `TABLE_PER_CLASS` : 구현 클래스마다 테이블 전략
  * `@DiscriminatorColumn(name="DTYPE")`
    * 부모 클래스에서 작성한다.
    * 자식 클래스들을 구별한 Column을 추가한다.
    * `DTYPE`은 항상 있는 것이 좋다.
      * `SINGLE_TABLE` 전략은 필수이다.
      * `JOINED` 전략은 필수는 아니지만, 있는 것이 좋다.
  * `@DiscriminatorValue("XXX")`
    * 자식 클래스에서 작성한다.
    * 부모 테이블 `DTYPE`에 들어갈 값을 지정한다.
    
### JOINED 전략
![](https://velog.velcdn.com/images/pipiolo/post/8f6e018c-79af-4084-a51a-381a42f8f142/image.png)

* 조인을 통해 조회하는 방법으로 가장 정석적인 방법이다.
* 장점
  * 정규화된 테이블 구조
  * 외래 키 잠조 무결성 제약조건 활용가능
  * 효율적인 저장 공간
* 단점
  * 조회시 조인을 사용한다. 성능 저하를 유발할 수 있다.
  * 데이터 저장시 INSERT SQL 2번 호출한다.

### SINGLE_TABLE 전략
![](https://velog.velcdn.com/images/pipiolo/post/d3134ab8-7fcb-4a85-946f-094c07d97b6e/image.png)

* 상속 관계를 하나의 테이블에 컬럼으로 설계하는 방법이다.
* 확장성이 필요없고 정말 단순한 구조에서 사용한다. 엔티티 구조가 변화하면 테이블의 많은 수정이 필요하다.
* `DTYPE` 속성이 필수이다. 없으면 자식 엔티티를 구별할 수 없다.
* 장점
  * 조인이 필요없으므로 일반적으로 성능이 좋다.
  * 조회 쿼리가 단순하다.
* 단점
  * 자식 엔티티가 매핑한 컬럼은 모두 `NULL`을 허용해야 한다.
    * Album 엔티티가 저장될 경우, 그외 Movie, Book 엔티티의 속성들은 모두 `NULL`이다.
  * 단일 테이블에 모든 매핑 정보를 입력하므로 테이블이 지나치게 커질 수 있다.

### TABLE_PER_CLASS 전략
![](https://velog.velcdn.com/images/pipiolo/post/63aa6ad8-52e2-4d4a-8dfa-5b2b5b8d8d1a/image.png)

* 구현 클래스마다 각각 테이블을 생성하는 방법이다.
* 부모 테이블(`Item`)이 없기 때문에 통합 관리가 어렵다.
* 객체 개발자, 데이터베이스 설계자 모두 싫어하는 전략이다. <span style="color: #FF8C00">사용하지 말자.</span>
* 장점
  * 서브 타입을 명확하게 구분해서 처리할 때 효과적이다.
  * `not null` 제약 조건이 가능하다.
* 단점
  * 여러 자식 테이블을 함께 조회할 때 성능이 느이다.
  * 자식 테이블들을 통합해서 쿼리하기 어렵다.
  
> **정리**
> 상속 관계 매핑 전략을 `옵션 1개`로 자유롭게 변경할 수 있다.
> 부모 엔티티(`Item`)는 인스턴스가 필요없으므로 추상 클래스(`abstract class`)로 만든다.

## @MappedSuperclass
```java
@MappedSuperclass
public abstract class BasicEntity {

  @Id @GeneratedValue
  private Long id;
  
  private LocalDateTime createdAt;
  
  private LocalDateTime updatedAt;
}
```
* 상속 받는 자식 클래스에 매핑 정보만 제공한다.
  * 엔티티가 아니다.
  * 별도 테이블이 생성되지 않는다.
  * 상속 관계 매핑 전략과 관련 없다.
* 전체 엔티티에서 공통적으로 필요한 정보들을 모아서 관리할 때 사용한다.
* 직접 생성할 일이 없으므로 추상 클래스(`abstract class`)로 사용하자.

> **참고**
> `@Entity` 클래스는 `@MappedSuperclass` 혹은 `@Entity` 클래스만 상속 가능하다.

---

# 프록시와 연관관계 정리

## 프록시 객체
![](https://velog.velcdn.com/images/pipiolo/post/3c88746b-61fa-4c70-b10c-aa900bba419e/image.png)

* 실제 클래스를 상속받아서 만들어진 클래스로 구조가 같다.
* 프록시 객체는 실제 객체의 참조(`target`)을 가지고 있다.
* 프록시 객체를 호출하면 프록시 내부에서 실제 객체의 메소드를 호출한다.
* 클라이언트는 진짜 객체인지 프록시 객체인지 구분하지 못 한다.

### 엔티티 조회
* `em.find()` → 데이터베이스를 통해서 **실제 엔티티 객체**를 조회한다.
* `em.getReference()` → 데이터베이스 조회를 미루는 **프록시 엔티티 객체**를 조회한다.
  * 데이터베이스에 쿼리를 보내지 않고 마치 실제 엔티티 객체가 있는 것처럼 행동한다.
  * 애플리케이션에서 실제 데이터를 사용할 때, 데이터베이스에 쿼리를 날린다.

### 프록시 객체의 초기화
![](https://velog.velcdn.com/images/pipiolo/post/02d309fe-c2c1-4f4c-8e0c-10918223a65d/image.png)

* 프록시 객체는 처음 사용할 때 한 번만 초기화된다.
* `초기화`란 프록시 객체의 `target` 참조에 실제 객체를 넣는 행위를 말한다.
  * `영속성 컨텍스트`를 통해 엔티티를 얻는다.
* 프록시 객체도 `영속성 컨텍스트`에 의해 `동일성`을 보장받는다.
  * `영속성 컨텍스트`에 실제 엔티티 객체가 있다면, `em.getReference()`를 호출해도 프록시 객체가 아닌 실제 엔티티 객체를 얻는다.
  * `em.getReference()`를 통해서 이미 프록시 객체를 받았다면, `em.find()`를 호출해도 실제 엔티티 객체가 아닌 프록시 객체를 얻는다.
  * 엔티티 객체의 타입 체크는 `==`가 아닌 `instance of`를 사용해야 한다.
    * 실제 객체인지 프록시 객체인지 구별하지 않기 때문이다.
* 준영속 상태 엔티티는 프록시 초기화 시, 예외가 발생한다.
  * 준영속 상태 엔티티는 `영속성 컨텍스트`가 관리하지 않기 때문에 초기화가 불가능하다.
  
## 즉시 로딩과 지연 로딩
<span style="color: #FF8C00">실무에서는 지연 로딩만 사용하자.</span>

* `즉시 로딩` → 연관관계에 있는 실제 객체까지 조회한다.
* `지연 로딩` → 연관관계 엔티티들은 프록시 객체로 조회한다. 프록시 객체의 `target`이 호출될 때, 영속성 컨텍스트에서 실제 엔티티 객체를 얻는다.

```java
@Entity
public class Team {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "team")
  private List<Member> members = new ArrayList<>();
}

@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;
}
```
* `Team`을 조회할 때 무조건 `Member` 엔티티까지 조회할 필요 없다.
* `즉시 로딩`은 예상하지 못한 SQL을 생성한다.
  * `select m from Member m` 
  * `Member`만 조회했는데 연관관계에 있는 `Team`을 조인하는 쿼리가 데이터베이스에 날라간다.
  * 연관관계 엔티티가 늘어날수록, 조인 쿼리가 증가한다.
* `즉시 로딩`은 JPQL에서 `N+1 문제`가 발생한다.
* `@ManyToOne`, `@OneToOne`은 디폴트로 `즉시 로딩`이 작동한다. → `지연 로딩`으로 변경하자.
* `@OneToMany`, `@ManyToMany`는 디폴트로 `지연 로딩`이 작동한다.
* <span style="color: #FF8C00">모든 연관관계에 지연 로딩만 사용하자.</span>

## 영속성 전이 (Cascade)
![](https://velog.velcdn.com/images/pipiolo/post/362340d6-bf43-4ec8-b0cc-c03ff77bbf4f/image.png)

* 특정 엔티티를 영속 상태로 만들 때, 연관된 엔티티도 함께 영속 상태도 만들고 싶을 때 사용한다.
* 특정 엔티티를 영속화할 때, 연관된 엔티티도 함께 영속화하는 편리함을 제공한다.
  * `em.persist()`를 `parent` 1번, `child` 2번 총 3번이 필요하지만, 
     영속성 전이를 사용하면 `parent` 1번으로 `child`까지 자동으로 영속화 해준다.
  * 데이터베이스에 나가는 쿼리는 동일하기 때문에 성능상 이점은 없다.
* 영속성 전이는 상속과 관련이 없다.
  * `Parent`는 상속 관계가 아닌, 연관관계에서 `일`을 담당하는 엔티티를 말한다.
  * `Team` : `Member` = 1 : 多 
     → `Parent`는 `Team`을 의미한다.

### 영속성 전이 종류
* **ALL**
* **PERSIST**
* **REMOVE**
* MERGE
* REFRESH
* DETACH

#### CascadeType.PERSIST
* 특정 엔티티를 영속화하면 연관된 엔티티도 함께 영속화한다.
  * 엔티티를 저장할 때, 연관된 모든 엔티티들이 영속 상태이어야 한다.
  * 이 속성을 사용하면 부모와 자식을 따로 영속화해주지 않아도 된다.
* 부모를 삭제하면 연관된 자식은 그대로 남아있다.

#### CascadeType.REMOVE
* 특정 엔티티를 삭제하면 연관된 엔티티도 함께 삭제한다.
* 특정 엔티티를 삭제했을 때, 연관된 다른 엔티티들은 외래 키 무결성 예외가 발생한다.
  * 연관된 테이블의 외래 키로 연결된 `Column`이 삭제되었기 때문이다.
  * `CascadeType.REMOVE` 옵션을 사용하지 않으면, 특정 엔티티를 지울 때 연관된 엔티티과 연관관계를 끊어야 한다.

#### CascadeType.ALL
* `CascadeType`이 제공하는 모든 속성을 적용한다.
* 연관된 엔티티들의 생명주기를 제어할 때 사용한다.

### 고아 객체
```java
@Entity
public class Parent {

  @Id @GeneratedValue
  private Long id;

  @OneToMany(mappedBy = "parent", orphanRemoval = true)
  private List<Child> children = new ArrayList<>();
}

@Entity
public class Child {

  @Id @GeneratedValue
  private Long id;

  @ManyToOne
  @JoinColumn(name = "parent_id")
  private Parent parent;
}
```

* `고아 객체`란 부모 객체와 연관관계가 끊어진 자식 객체를 말한다.
* `orphanRemoval = true`을 하면, 부모 엔티티와 연관관계가 끊어진 자식 엔티티를 자동으로 제거한다.
  * `parent.getChildren().remove()`
     → 자식 엔티티를 컬렉션에서 제거하면, 자동으로 `DELETE SQL` 생성한다.
* 자식 엔티티가 부모 엔티티에 종속되는 경우에만 사용한다.
  * 특정 엔티티가 개인 소유할 때 사용가능하다. 
  * 참조하는 곳이 하나일 때 사용가능하다.
* `@OneToOne`, `@OneToMany` 연관관계에서만 가능하다.
* 예) 특정 게시판을 삭제하면, 해당 게시판에 달린 댓글들을 함께 삭제된다.

> **참고**
> 부모 객체가 제거되면 자식 객체는 고아가 된다. 고아 객체 제거 기능을 활성화하면, 부모 객체를 제거하면 자식 객체도 함께 제거된다. `CascadeType.REMOVE`처럼 동작한다.

> **참고**
> `CascadeType.REMOVE`는 부모 엔티티가 제거되면 자식 엔티티를 제거한다.
> `orphanRemoval = true`은 부모 엔티티와의 연관관계가 끊어지면 고아가 된 자식 엔티티를 제거한다.
> 부모 엔티티를 제거할 때는 동일하게 동작하지만, `orphanRemoval = true`은 부모 엔티티가 살아있어도 자식 엔티티를 지울 수 있다.

### 영속성 전이 + 고아 객체
* `CascadeType.ALL` + `orphanRemoval = true`
* 부모 엔티티를 통해서 자식의 생명 주기를 관리할 수 있다.
  * `Child`는 `Repository`가 없어도 `Parent`를 통해 관리할 수 있다.

---

# 값 타입 
## JPA 데이터 타입 분류
* 엔티티 타입
  * `@Entity` 정의하는 객체
  * 데이터가 변해도 식별자(`@Id`)로 추적 가능하다.
  * 예) 회원 엔티티의 이름, 나이 등 데이터가 변경되어도 식별자를 통해 조회할 수 있다.
* 값 타입
  * int, String 등 값으로 사용하는 자바 기본 타입 혹은 객체
  * 식별자가 없어, 데이터 변경시 추적이 불가능하다.
  * 값이 변경되면 완전히 다른 것으로 인식한다.
    
## 값 타입 이해
* `생명 주기`를 엔티티에 의존한다.
* 값 타입은 `공유`되면 안 된다.
  * 예) 특정 회원의 이름 변경 시, 다른 회원의 이름이 변경되면 안 된다.
* 값 타입 분류
  * 기본 값 타입
    * 자바 기본 타입 (int, double)
    * 래퍼 클래스 (Integer, Long)
    * String
  * 임베디드 타입 (복합 값 타입)
  * 컬렉션 값 타입

### 값 타입과 불변 객체
* 값 타입을 여러 엔티티에서 공유하면 위험한다.
* 값 타입은 항상 복사해서 사용해야 한다.
  * 공유 참조로 인한 부작용을 막을 수 있다.

#### 객체 타입의 한계
* 래퍼 클래스, 임베디트 타입 등 값 타입 중 자바 기본 타입이 아닌 객체 타입이 있다.
* 객체 타입은 공유 참조를 피할 수 없다.
  * int, double 등 자바 기본 타입은 항상 값을 복사한다.
  * Interger, String 등 객체 타입은 항상 참조 값을 넘긴다.

#### 불변 객체
* 생성 이후 값을 변경할 수 없는 객체인 `불변 객체`로 설계한다.
  * 객체 타입을 수정할 수 없게 만들어 부작용을 원천 차단한다.
  * 생성자로만 값을 설정하고 수정자(`setter`)를 만들지 않는다.
* 공유 참조는 막을 수 없지만, 변경할 수 없게 만들어 부작용을 차단한다.
* 래퍼 클래스 및 String 클래스는 자바가 제공하는 대표적인 불변 객체이다.

### 값 타입 비교
* 값 타입은 인스턴스가 달라도 값이 같으면 같은 것으로 봐야한다.
* 동일성 비교 → `==` 인스턴스 참조 값을 비교한다.
* 동등성 비교 → `equals()` 인스턴스 값을 비교한다.
* 값 타입은 항상 `동등성` 비교를 해야한다.
  * `equals()` 기본 구현은 `==` 을 통한 참조 값 비교이다.
  * 값 타입은 `equals()` 재정의 해야한다.
    * 직접 메소드를 오버라이드하지 말고, 인텔리제이를 통해서 재정의하자.
    * 프러퍼티 접근법이 아닌 `getXXX()`로 접근하자.
  * 항상 `hashcode()` 재정의 해야한다. 자바 컬렉션에서 값을 찾을 때 사용하기 때문이다.

## 기본 값 타입
* `값 타입`이기 때문에 `생명 주기`를 엔티티에 의존한다.
* 다른 엔티티와 `공유`되면 안 된다.
* int, double 등 자바 기본 타입은 공유가 불가능하다. 항상 값을 복사한다.
* Integer 등 `래퍼 클래스` 및 `String 클래스`는 공유는 가능하지만 값을 변경할 수 없다.
  * 객체이기 때문에 공유 참조를 막을 수 없다.
  * 값을 변경할 수 없는 `불변 객체`로 설계해서 변경을 막는다.

## 임베디드 타입 (복합 값 타입)
```java
@Entity
class Member {

    @Id @GeneratedValue
    private Long id;

    private String name;
    
    @Embedded
    private Address address;
}
  
@Embeddable
class Address {

    private String city;
    private String street;
    private String zipcode;
    
    public Address() {
    }
}
```

* 새로운 값 타입을 직접 정의한다.
  * 주로 기본 값 타입을 모아서 `복합 값 타입`을 정의한다.
* `생명 주기`는 엔티티에 의존한다.
* 다른 엔티티와 공유되면 안 된다.
* 임베디드 타입 사용법
  * `@Embedded` → 임베디트 타입 사용하는 곳
  * `@Embeddable` → 임베디드 타입 정의하는 곳
  * 임베디트 타입 클래스는 기본 생성자 필수이다.
* 임베디트 타입은 엔티티의 값일 뿐이다. **엔티티가 아니다.**
* 임베디드 타입을 사용하든 안 하든 테이블 형태는 동일하다.
  * 데이터베이스에 영향을 주지 않는다.
* 장점
  * 재사용
  * 높은 응집도
  * 의미 있는 메소드
  
## 값 타입 컬렉션
```java
@Entity
public class Member {

  @Id @GeneratedValue
  private Long id;
  
  @ElementCollection
  @CollectionTable(name = "foods", joinColumns = 
          @JoinColumn(name = "member_id"))
  private List<String> foods = new ArrayList<>();
}
```
* 값 타입이 들어간 자바 컬력센을 저장할 때 사용한다.
  * `@ElementCollection`, `@CollectionTable`
* 컬렉션을 저장하기 위한 별도의 테이블이 필요하다.
  * 데이터베이스는 컬렉션을 같은 테이블에 저장할 수 없다.
  * 컬렉션의 크기에 따라 테이블의 구조가 계속해서 변화해야 한다.
* 식별자가 없다. → 추적이 불가능하다.
  * 값 타입 컬렉션에 변경사항이 발생하면, 해당 엔티티와 연관된 데이터를 삭제하고 다시 저장한다.
  * 컬렉션 테이블에 연관된 데이터들을 모두 삭제하고 처음부터 저장한다.
  * `UPDATE SQL`이 아닌, `DELETE SQL` + `SQLECT SQL`로 변경사항을 반영한다.
* <span style="color: #FF8C00">사용하지 말자.</span>
  * 일대다 관계(`@OneToMany`)를 사용하자.
  * 영속성 전이(`CascadeType.ALL`) + 고아 객체 제거(`orphanRemoval = true`)로 값 타입 컬렉션처럼 사용하자.

> **정리**
> * 엔티티 타입
>   * 식별자가 있다. → 추적 가능하다.
>   * 생명 주기를 관리한다.
>   * 공유 참조한다.
> * 값 타입
>   * 식별자가 없다. → 추적 불가능하다.
>   * 생명 주기를 엔티티에 의존한다.
>   * 공유하지 않고 값 복사해서 사용한다.
>   * 객체 타입은 불변 객체로 만든다.

---
