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