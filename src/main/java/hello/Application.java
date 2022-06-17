package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.List;
import java.util.Map;

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

  private String makeDecision(ArenaUpdate arenaUpdate) {
      PlayerState me = arenaUpdate.arena.state.remove(arenaUpdate._links.self.href);

      if(canScore(me, arenaUpdate)) {
        System.out.println("T");
          return "T";
      } else {
          String m = makeMoveAction(me, arenaUpdate);
          System.out.println(m);
          return m;
      }
  }

  private String makeMoveAction(PlayerState me, ArenaUpdate arenaUpdate) {
      List<Integer> dims = arenaUpdate.arena.dims;
      if("W".equalsIgnoreCase(me.direction) && me.x == 0) {
          if(me.y > dims.get(1)/2) return "R";
          else return "L";
      }
      if("E".equalsIgnoreCase(me.direction) && me.x >= dims.get(0) -1 ) {
          if(me.y > dims.get(1)/2) return "L";
          else return "R";
      };
      if("N".equalsIgnoreCase(me.direction) && me.y == 0) {
          if(me.x > dims.get(0)/2) return "L";
          else return "R";
      }
      if("S".equalsIgnoreCase(me.direction) && me.y >= dims.get(1) - 1) {
          if(me.x > dims.get(0)/2) return "R";
          else return "L";
      }

      return "F";
  }

  private boolean canScore(PlayerState me, ArenaUpdate arenaUpdate) {
      return arenaUpdate.arena.state.entrySet().stream().anyMatch(entry -> {
          PlayerState p = entry.getValue();
          if(Objects.equals(p.y, me.y)) {
             if("E".equalsIgnoreCase(me.direction) && p.x - me.x <= 3 && p.x > me.x) {
                 return true;
             } else if("W".equalsIgnoreCase(me.direction) && me.x - p.x <= 3 && p.x <= me.x) {
                 return true;
             }
          } else if(Objects.equals(p.x, me.x)) {
              if("S".equalsIgnoreCase(me.direction) && p.y - me.y <= 3 && p.y > me.y) {
                  return true;
              } else if("N".equalsIgnoreCase(me.direction) && me.y - p.y <= 3 && p.y < me.y) {
                  return true;
              }
          }

          return false;
      });
  }

}

