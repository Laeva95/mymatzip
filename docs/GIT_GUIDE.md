# mymatzip Git 협업 가이드

## 브랜치 구조

```
main            ← 배포용. 배포 직전에 develop을 딱 한 번 merge
 └─ develop     ← 개발 통합용. 각자의 작업을 Pull Request로 merge
     ├─ feature/김지현
     ├─ feature/서희정
     ├─ feature/유승민
     ├─ feature/정범석
     └─ feature/정운학   ← 각자 개발하는 개인 브랜치
```

- 개발은 **자기 개인 브랜치(`feature/본인이름`)에서만** 합니다.
- `develop`과 `main`에는 직접 push하지 않고, GitHub의 **Pull Request**로만 merge합니다.
- 아래 예시는 `feature/김지현` 기준입니다. **본인 브랜치 이름으로 바꿔서** 입력하세요.

## 먼저 알아둘 개념

- **원격 저장소(origin)**: GitHub에 있는 저장소입니다. 팀원 모두가 같이 씁니다.
- **로컬 저장소**: 내 컴퓨터에 복사해 온 저장소입니다. 내가 commit해도 push하기 전까지는 내 컴퓨터에만 있습니다.

작업 흐름은 **로컬에서 작업 → commit(내 컴퓨터에 저장) → push(GitHub에 올리기)** 순서입니다.

## 전체 흐름 한눈에 보기

| 단계 | 내용 | 언제 |
|---|---|---|
| 0단계 | 내 이름과 이메일 등록 | 처음 한 번만 |
| 1단계 | 저장소 clone | 처음 한 번만 |
| 2단계 | 내 개인 브랜치로 이동, 인텔리제이로 열기 | 처음 한 번만 |
| 3단계 | develop의 최신 내용을 내 브랜치로 가져오기 | **작업 시작 전마다** |
| 4단계 | 개발하고 GitHub에 push | 반복 |
| 5단계 | GitHub에서 develop으로 merge(Pull Request) | 반복 |

처음 한 번 0~2단계를 하고, 그다음부터는 **3 → 4 → 5단계를 반복**합니다.

---

## 0단계. 처음 한 번만: 내 이름과 이메일 등록

commit할 때 작성자로 기록될 정보입니다. 이메일은 GitHub 계정 이메일로 입력하세요.

```bash
git config --global user.name "김지현"
```

```bash
git config --global user.email "본인GitHub이메일@example.com"
```

`git pull`로 내용을 가져올 때 **merge 방식으로 합치도록** 정해 두는 설정입니다. 평소에는 필요 없지만, 실수로 내 컴퓨터의 `develop`에서 commit한 상태로 `git pull`을 하면 `fatal: Need to specify how to reconcile divergent branches` 오류가 나며 멈출 수 있어서 미리 해 둡니다.

```bash
git config --global pull.rebase false
```

## 1단계. 처음 한 번만: 저장소 clone 하기

clone할 폴더(예: 인텔리제이 기본 폴더인 `IdeaProjects`)로 이동한 뒤 실행합니다.

```bash
git clone https://github.com/Laeva95/mymatzip.git
```

```bash
cd mymatzip
```

clone하면 GitHub에 있는 모든 브랜치 정보를 가져오지만, 바로 쓸 수 있는 로컬 브랜치는 `main` 하나뿐입니다. 아래 명령어로 확인할 수 있습니다.

```bash
git branch -a
```

`remotes/origin/feature/김지현`처럼 `remotes/`가 붙은 것은 GitHub에 있는 브랜치를 보여 주기만 하는 것이고, 아직 내 로컬 브랜치는 아닙니다.

## 2단계. 처음 한 번만: 내 개인 브랜치로 이동

```bash
git switch feature/김지현
```

GitHub에 같은 이름의 브랜치가 있으면 Git이 **로컬 브랜치를 새로 만들고 GitHub 브랜치와 연결**까지 해 줍니다. 연결된 덕분에 이후에는 `git push`, `git pull`만 입력해도 어느 브랜치로 보낼지 알아서 찾아갑니다.

> `git switch`가 안 되는 오래된 Git 버전이라면 `git checkout feature/김지현`으로 실행하세요.

지금 어느 브랜치에 있는지는 아래 명령어로 확인합니다. `*` 표시가 붙은 것이 현재 브랜치입니다.

```bash
git branch
```

### 인텔리제이로 열기

이 프로젝트는 **Gradle** 프로젝트입니다. 인텔리제이는 Gradle 프로젝트를 자동으로 알아보기 때문에 폴더만 열면 됩니다.

1. 인텔리제이에서 `File → Open`을 선택합니다. 시작 화면이라면 `Open` 버튼을 누릅니다.
2. clone한 `mymatzip` 폴더를 선택하고 `OK`를 누릅니다.
3. **Trust Project** 창이 나오면 `Trust Project`를 누릅니다.
4. 오른쪽 아래에 Gradle 진행 표시가 끝날 때까지 기다립니다. 처음에는 필요한 라이브러리를 내려받느라 몇 분 걸릴 수 있습니다.

> **인텔리제이 안에서 Git 명령어 입력하기**: 아래쪽의 **Terminal** 탭(단축키 `Alt + F12`, Mac은 `Option + F12`)을 열면 프로젝트 폴더에서 바로 이 문서의 git 명령어를 입력할 수 있습니다. 오른쪽 아래 상태 표시줄에는 **현재 브랜치 이름**이 항상 보이니, 작업 중 브랜치를 확인할 때도 활용하세요.

---

## 3단계. 작업 시작 전에: develop 내용을 내 브랜치로 가져오기 (반복)

다른 팀원이 develop에 merge한 내용을 내 브랜치에도 반영하는 단계입니다. 작업 전에 먼저 해 두면 내 코드가 오래된 코드 위에 쌓이지 않아 충돌이 줄어듭니다.

**① 내 작업이 남아 있지 않은지 확인하기**

```bash
git status
```

`nothing to commit, working tree clean`이 나오면 다음으로 넘어갑니다. 바뀐 파일이 남아 있으면 이 단계가 꼬일 수 있으니, **4단계의 ②~③(add와 commit)**을 먼저 하세요.

**② develop 브랜치로 이동**

```bash
git switch develop
```

처음 실행할 때는 2단계처럼 로컬 `develop` 브랜치가 자동으로 만들어지고 GitHub의 `develop`과 연결됩니다.

> 내 컴퓨터의 `develop` 브랜치는 **최신 내용을 받아 오는 용도로만** 씁니다. 여기서 코드를 수정하거나 commit하지 마세요.

**③ GitHub의 develop 최신 내용 가져오기**

```bash
git pull
```

**④ 다시 내 브랜치로 이동**

```bash
git switch feature/김지현
```

**⑤ develop 내용을 내 브랜치에 합치기**

```bash
git merge develop
```

- 합친 기록(merge commit)을 저장하는 편집기 화면이 뜨면, 그대로 저장하고 닫으면 됩니다. 인텔리제이 터미널에서 `vim` 화면이 뜨면 `:wq`를 입력하고 `Enter`를 누르세요.
- `CONFLICT`라는 메시지가 나오면 아래 [merge 충돌이 났을 때](#merge-충돌conflict이-났을-때)를 참고하세요.

**⑥ 합친 결과를 GitHub의 내 브랜치에도 올리기**

```bash
git push
```

## 4단계. 개발하고 GitHub에 push 하기 (반복)

코드를 수정한 뒤 실행합니다.

**① 무엇이 바뀌었는지 확인**

```bash
git status
```

**② commit할 파일 담기** (`.`은 바뀐 파일 전부를 뜻합니다)

```bash
git add .
```

**③ commit하기** (내 컴퓨터에 저장)

```bash
git commit -m "로그인 화면 구현"
```

**④ GitHub에 올리기**

```bash
git push
```

> push하기 전에 `git branch`로 **내 개인 브랜치에 있는지** 꼭 확인하세요. 실수로 develop이나 main에서 작업하는 경우가 가장 흔합니다.

## 5단계. GitHub에서 develop으로 merge 하기 (Pull Request)

1. GitHub 저장소 페이지에서 **Pull requests** 탭을 누르고 **New pull request**를 클릭합니다.
2. 브랜치를 이렇게 고릅니다.
   - **base: `develop`** ← 합쳐질 곳
   - **compare: `feature/김지현`** ← 내 작업
   - 기본값이 base: `main`으로 되어 있으니 **꼭 `develop`으로 바꾸세요.**
3. **Create pull request**를 누르고 제목과 설명을 적습니다.
4. 팀원 확인이 끝나면 **Merge pull request → Confirm merge**를 누릅니다.

이제 다음 작업을 시작할 때 다시 3단계부터 진행하면 됩니다.

---

## merge 충돌(conflict)이 났을 때

두 사람이 **같은 파일의 같은 부분**을 고치면 Git이 어느 쪽을 남길지 정하지 못해 `CONFLICT`라는 메시지를 띄웁니다. 그러면 해당 파일 안에 이런 표시가 생깁니다.

```
<<<<<<< HEAD
내가 작성한 코드
=======
develop에 있던 코드
>>>>>>> develop
```

1. 인텔리제이에서 그 파일을 열고, 남길 코드만 남긴 다음 `<<<<<<<`, `=======`, `>>>>>>>` 줄을 지웁니다.
   - 인텔리제이의 충돌 해결 화면을 써도 됩니다. 메뉴에서 `Git → Resolve Conflicts...`를 선택하고 파일을 더블클릭하면, 왼쪽(내 코드)·가운데(결과)·오른쪽(develop 코드)이 나란히 보입니다. 화살표(`>>`, `<<`)를 눌러 남길 코드를 가운데로 옮기고 `Apply`를 누르면 됩니다.
2. 아래처럼 다시 commit하고 push합니다.

```bash
git add .
```

```bash
git commit -m "develop merge 충돌 해결"
```

```bash
git push
```

충돌을 줄이려면 **역할(파일) 분담을 확실히 하고, develop 내용을 자주 가져오는 것**이 가장 효과적입니다.

## push가 거절됐을 때

`develop`과 `main`은 직접 push할 수 없도록 보호되어 있습니다. 아래 메시지가 나오면 **브랜치를 잘못 고른 것**입니다.

```
remote: error: GH006: Protected branch update failed for refs/heads/develop.
remote: error: Changes must be made through a pull request.
 ! [remote rejected] develop -> develop (protected branch hook declined)
```

`git branch`로 현재 브랜치를 확인하고, 내 개인 브랜치(`feature/본인이름`)로 이동해서 작업하세요.

## 마지막: 배포 전 main에 merge 하기 (관리자)

5단계와 같은 방법으로 Pull Request를 만들고, **base: `main` / compare: `develop`**으로 선택해서 merge합니다.

## 자주 쓰는 명령어 정리

| 명령어 | 하는 일 |
|---|---|
| `git status` | 바뀐 파일 확인 |
| `git branch` | 현재 브랜치 확인 (`*` 표시) |
| `git switch 브랜치이름` | 브랜치 이동 |
| `git add .` | 바뀐 파일 전부 commit 대상에 담기 |
| `git commit -m "메시지"` | 내 컴퓨터에 저장 |
| `git push` | GitHub에 올리기 |
| `git pull` | GitHub의 최신 내용 가져오기 |
| `git merge develop` | develop 내용을 현재 브랜치에 합치기 |
