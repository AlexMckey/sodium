import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import sodium.*;

public class HomoSapiens {
    public HomoSapiens(
        World world,
        int self,
        double tInit,
        Point posInit,
        Cell<Double> time,
        Stream<Unit> sTick)
    {
        final double speed = 80.0;
        final double step = 0.02;
        class Trajectory {
            Trajectory(World world, Random rng, double t0, Point orig) {
                this.t0 = t0;
                this.orig = orig;
                this.period = rng.nextDouble() * 1 + 0.5;
                for (int i = 0; i < 10; i++) {
                    double angle = rng.nextDouble() * Math.PI * 2;
                    velocity = new Vector(Math.sin(angle), Math.cos(angle))
                                .mult(speed);
                    if (!world.hitsObstacle(positionAt(t0 + step*2)))
                        break;
                }
            }
            double t0;
            Point orig;
            double period;
            Vector velocity;
            Point positionAt(double t) {
                return velocity.mult(t - t0).add(orig);
            }
        }
        class All {
            All(Trajectory traj, double t) {
                this.traj = traj;
                this.t = t;
            }
            final Trajectory traj;
            final double t;
        };

        Random rng = new Random();
        CellLoop<Trajectory> traj = new CellLoop<>();
        Cell<All> all = Cell.lift(
            (tr, clk) -> new All(tr, clk), traj, time);
        Stream<Unit> sChange = Stream.filterOptional(
            sTick.snapshot(all,
                (u, a) -> {
                    if (world.hitsObstacle(a.traj.positionAt(a.t + step))
                        || a.t - a.traj.t0 >= a.traj.period)
                        return Optional.of(Unit.UNIT);
                    else
                        return Optional.<Unit>empty();
                }));
        traj.loop(
            sChange.snapshot(all, (u, a) -> {
                return new Trajectory(world, rng, a.t,
                                      a.traj.positionAt(a.t));
            }).hold(new Trajectory(world, rng, tInit, posInit))
        );
        character = all.map(a -> {
                return new Character(self, CharacterType.SAPIENS,
                    a.traj.positionAt(a.t), a.traj.velocity);
            });
    }

    public final Cell<Character> character;
}

