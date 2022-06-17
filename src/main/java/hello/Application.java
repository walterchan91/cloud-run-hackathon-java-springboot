package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@RestController
public class Application {

  static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    return makeDecision(arenaUpdate);
  }

  final AtomicInteger hitCount = new AtomicInteger(0);

  private String makeDecision(ArenaUpdate arenaUpdate) {
    PlayerState me = arenaUpdate.arena.state.remove(arenaUpdate._links.self.href);

    if (me.wasHit && hitCount.incrementAndGet() >= 3) {
      escape(me, arenaUpdate);
    } else if (me.wasHit && lastScore.get() - me.score >= 2) {
      escape(me, arenaUpdate);
    }

    lastScore.set(me.score);
    if (!me.wasHit)
      hitCount.set(0);
    else
      hitCount.incrementAndGet();

    if (canScore(me, arenaUpdate)) {
      System.out.println("T");
      return "T";
    } else {
      String m = makeMoveAction(me, arenaUpdate);
      System.out.println(m);
      return m;
    }
  }

  final static AtomicInteger lastScore = new AtomicInteger(10000000);

  private String escape(PlayerState me, ArenaUpdate arenaUpdate) {
    int[] forwardPos = forwardPos(me.direction, me.x, me.y);

    if (isValidPos(forwardPos, arenaUpdate)) {
      if (isPlaceTaken(arenaUpdate, forwardPos)) {
        // forward not blocked
        System.out.println("Escape forward");
        return "F";
      }
    }

    // can not forward
    // try left
    int[] leftPos = forwardPos(findLeftDirection(me.direction), me.x, me.y);
    if (isValidPos(leftPos, arenaUpdate)) {
      System.out.println("Escape left");
      return "L";
    }
    // try right
    String rightDirection = findRightDirection(me.direction);
    int[] rightPos = forwardPos(rightDirection, me.x, me.y);
    if (isValidPos(rightPos, arenaUpdate)) {
      System.out.println("Escape right");
      return "R";
    }

    // in the back?
    int[] backPos = forwardPos(findRightDirection(rightDirection), me.x, me.y);
    if (isValidPos(backPos, arenaUpdate)) {
      System.out.println("turn R to escape to back");
      return "R";
    }

    System.out.println("Stucked!!");
    // no solution
    return "T";
  }

  private boolean isValidPos(int[] forwardPos, ArenaUpdate arenaUpdate) {
    return forwardPos[0] >= 0 && forwardPos[0] <= arenaUpdate.arena.dims.get(0) - 1 && forwardPos[1] >= 0
        && forwardPos[1] <= arenaUpdate.arena.dims.get(1) - 1;
  }

  private String findLeftDirection(String direction) {
    switch (direction) {
      case "N":
        return "W";
      case "S":
        return "E";
      case "W":
        return "S";
      case "E":
        return "N";
    }
    return direction;
  }

  private String findRightDirection(String direction) {
    switch (direction) {
      case "N":
        return "E";
      case "S":
        return "W";
      case "W":
        return "N";
      case "E":
        return "S";
    }
    return direction;
  }

  private int[] forwardPos(String direction, int cx, int cy) {
    int[] forwardPos = new int[2];
    switch (direction) {
      case "S":
        forwardPos[0] = cx;
        forwardPos[1] = cy + 1;
        break;
      case "N":
        forwardPos[0] = cx;
        forwardPos[1] = cy - 1;
        break;
      case "E":
        forwardPos[0] = cx + 1;
        forwardPos[1] = cy;
        break;
      case "W":
        forwardPos[0] = cx - 1;
        forwardPos[1] = cy;
        break;
    }

    return forwardPos;
  }

  private boolean isPlaceTaken(ArenaUpdate arenaUpdate, int[] forwardPos) {
    return arenaUpdate.arena.state.values().stream().noneMatch(p -> p.x == forwardPos[0] && p.y == forwardPos[1]);
  }

  private boolean targetInFront(String direction, PlayerState me, Arena arena) {
    switch (direction) {
      case "N":
        return arena.state.values().parallelStream().anyMatch(p -> p.x == me.x && p.y < me.y);
      case "S":
        return arena.state.values().parallelStream().anyMatch(p -> p.x == me.x && p.y > me.y);
      case "W":
        return arena.state.values().parallelStream().anyMatch(p -> p.x < me.x && p.y == me.y);
      case "E":
        return arena.state.values().parallelStream().anyMatch(p -> p.x > me.x && p.y == me.y);
    }

    return false;
  }

  private String makeMoveAction(PlayerState me, ArenaUpdate arenaUpdate) {
    List<Integer> dims = arenaUpdate.arena.dims;
    if ("W".equalsIgnoreCase(me.direction) && me.x == 1) {
      if (me.y > dims.get(1) / 2)
        return "R";
      else
        return "L";
    }
    if ("E".equalsIgnoreCase(me.direction) && me.x >= dims.get(0) - 2) {
      if (me.y > dims.get(1) / 2)
        return "L";
      else
        return "R";
    }
    ;
    if ("N".equalsIgnoreCase(me.direction) && me.y == 1) {
      if (me.x > dims.get(0) / 2)
        return "L";
      else
        return "R";
    }
    if ("S".equalsIgnoreCase(me.direction) && me.y >= dims.get(1) - 2) {
      if (me.x > dims.get(0) / 2)
        return "R";
      else
        return "L";
    }

    if (targetInFront(me.direction, me, arenaUpdate.arena))
      return "F";
    else {
      if (targetInFront(findLeftDirection(me.direction), me, arenaUpdate.arena)) {
        System.out.println("find target in left, turn L");
        return "L";
      } else if (targetInFront(findRightDirection(me.direction), me, arenaUpdate.arena)) {
        System.out.println("find target in right, turn R");
        return "R";
      }
      System.out.println("empty space, I'll bet");
      return new Random().nextBoolean() ? "T" : "F";
    }
  }

  private boolean canScore(PlayerState me, ArenaUpdate arenaUpdate) {
    return arenaUpdate.arena.state.entrySet().stream().anyMatch(entry -> {
      PlayerState p = entry.getValue();
      if (Objects.equals(p.y, me.y)) {
        if ("E".equalsIgnoreCase(me.direction) && p.x - me.x <= 3 && p.x > me.x) {
          return true;
        } else if ("W".equalsIgnoreCase(me.direction) && me.x - p.x <= 3 && p.x <= me.x) {
          return true;
        }
      } else if (Objects.equals(p.x, me.x)) {
        if ("S".equalsIgnoreCase(me.direction) && p.y - me.y <= 3 && p.y > me.y) {
          return true;
        } else if ("N".equalsIgnoreCase(me.direction) && me.y - p.y <= 3 && p.y < me.y) {
          return true;
        }
      }

      return false;
    });
  }

}
